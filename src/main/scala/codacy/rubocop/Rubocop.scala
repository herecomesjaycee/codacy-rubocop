package codacy.rubocop

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import codacy.dockerApi._
import codacy.dockerApi.utils.CommandRunner
import play.api.libs.json._

import scala.io.Source
import scala.util.{Failure, Properties, Success, Try}

object Rubocop extends Tool {

  // Gemfile is analysed
  private val filesToIgnore: Set[String] = Set("Gemfile.lock").map(_.toLowerCase())

  override def apply(path: Path, conf: Option[List[PatternDef]], files: Option[Set[Path]])(implicit spec: Spec): Try[List[Result]] = {
    val cmd = getCommandFor(path, conf, files, spec, resultFilePath)
    CommandRunner.exec(cmd, Some(path.toFile)) match {
    
      case Right(resultFromTool) if resultFromTool.exitCode < 2 =>
        parseResult(resultFilePath.toFile) match {
          case s@Success(_) => s
          case Failure(e) =>
            val msg =
              s"""
                 |Rubocop exited with code ${resultFromTool.exitCode}
                 |message: ${e.getMessage}
                 |stdout: ${resultFromTool.stdout.mkString(Properties.lineSeparator)}
                 |stderr: ${resultFromTool.stderr.mkString(Properties.lineSeparator)}
                """.stripMargin
            Failure(new Exception(msg))
        }

      case Right(resultFromTool) =>
        val msg =
          s"""
             |Rubocop exited with code ${resultFromTool.exitCode}
             |stdout: ${resultFromTool.stdout.mkString(Properties.lineSeparator)}
             |stderr: ${resultFromTool.stderr.mkString(Properties.lineSeparator)}
                """.stripMargin
        Failure(new Exception(msg))

      case Left(e) =>
        Failure(e)
    }
  }

  private[this] def parseResult(filename: File): Try[List[Result]] = {
    Try {
      val resultFromTool = Source.fromFile(filename).getLines().mkString
      Json.parse(resultFromTool)
    }.flatMap { json =>
      json.validate[RubocopResult] match {
        case JsSuccess(rubocopResult, _) =>
          Success(
            rubocopResult.files.getOrElse(List.empty).flatMap {
              file => ruboFileToResult(file)
            }
          )
        case JsError(err) =>
          Failure(new Throwable(Json.stringify(JsError.toFlatJson(err))))
      }
    }
  }

  private[this] def ruboFileToResult(rubocopFiles: RubocopFiles): List[Result] = {
    rubocopFiles.offenses.getOrElse(List.empty).map { offense =>
      Issue(SourcePath(rubocopFiles.path.value), ResultMessage(offense.message.value),
        PatternId(getIdByPatternName(offense.cop_name.value)), ResultLine(offense.location.line))
    }
  }

  private[this] def getCommandFor(path: Path, conf: Option[List[PatternDef]], files: Option[Set[Path]], spec: Spec, outputFilePath: Path): List[String] = {
    val configPath = conf.flatMap(getConfigFile(_).map { configFile =>
      List("-c", configFile.toAbsolutePath.toString)
    }).getOrElse(List.empty)

    val patternsCmd = (for {
      patterns <- conf.getOrElse(List.empty)
    } yield getPatternNameById(patterns.patternId)) match {
      case patterns if patterns.nonEmpty => List("--only", patterns.mkString(","))
      case _ => List.empty
    }

    val filesCmd = files.getOrElse(List(path.toAbsolutePath))
      .collect {
        case file if !filesToIgnore.contains(file.getFileName.toString.toLowerCase()) =>
          file.toString
      }

    List("rubocop", "--force-exclusion", "-f", "json", "-o", outputFilePath.toAbsolutePath.toString) ++ configPath ++ patternsCmd ++ filesCmd
  }

  private[this] lazy val resultFilePath = Paths.get(Properties.tmpDir, "rubocop-result.json")

  private[this] def getConfigFile(conf: List[PatternDef]): Option[Path] = {
    val rules = for {
      pattern <- conf
    } yield generateRule(pattern.patternId, pattern.parameters)

    val ymlConfiguration =
      s"""
         |AllCops:
         |  Include:
         |    - '**/*.rb'
         |    - '**/*.arb'
         |    - '**/*.axlsx'
         |    - '**/*.gemfile'
         |    - '**/*.gemspec'
         |    - '**/*.jbuilder'
         |    - '**/*.opal'
         |    - '**/*.podspec'
         |    - '**/*.rake'
         |    - '**/buildfile'
         |    - '**/Berksfile'
         |    - '**/Capfile'
         |    - '**/Cheffile'
         |    - '**/Fastfile'
         |    - '**/*Fastfile'
         |    - '**/Gemfile'
         |    - '**/Guardfile'
         |    - '**/Podfile'
         |    - '**/Rakefile'
         |    - '**/Thorfile'
         |    - '**/Vagabondfile'
         |    - '**/Vagrantfile'
         |  Exclude:
         |    - "vendor/**/*"
         |    - "db/schema.rb"
         |    - ".git/**/*"
         |  DisplayCopNames: false
         |  StyleGuideCopsOnly: false
         |  UseCache: false
         |${rules.mkString(s"${Properties.lineSeparator}")}
      """.stripMargin

    fileForConfig(ymlConfiguration).toOption
  }

  private[this] def fileForConfig(config: String) = tmpFile(config.toString)

  private[this] def tmpFile(content: String, prefix: String = "config", suffix: String = ".yml"): Try[Path] = {
    Try {
      Files.write(
        Files.createTempFile(prefix, suffix),
        content.getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE
      )
    }
  }

  private[this] def generateRule(patternId: PatternId, parameters: Option[Set[ParameterDef]]): String = {
    val ymlProperties = parameters.map {
      parameterDef =>
        parameterDef.map {
          pattern => generateParameter(pattern)
        }
    }.getOrElse(Set.empty)
    val patternConfig =
      s"""
         |${getPatternNameById(patternId)}:
         |  Enabled: true
         |  ${ymlProperties.mkString(s"${Properties.lineSeparator}  ")}
    """.stripMargin

    if (parameters.nonEmpty) {
      patternConfig
    } else {
      ""
    }
  }


  private[this] def getPatternNameById(patternId: PatternId): String = {
    patternId.value.replace('_', '/')
  }

  private[this] def getIdByPatternName(id: String): String = {
    id.replace('/', '_')
  }

  private[this] def generateParameter(parameter: ParameterDef): String = {
    parameter.value match {
      case JsArray(parameters) if parameters.nonEmpty =>
        val finalParameters = parameters.map(p => s"    - ${Json.stringify(p)}")
          .mkString(Properties.lineSeparator)
        s"""${parameter.name.value}:
           |$finalParameters
         """.stripMargin

      case JsArray(parameters) =>
        s"""${parameter.name.value}: []
         """.stripMargin

      case other => s"${parameter.name.value}: ${Json.stringify(other)}"
    }
  }

}
