
Checks for if and unless statements that would fit on one line
if written as a modifier if/unless. The maximum line length is
configured in the `Metrics/LineLength` cop. The tab size is configured
in the `IndentationWidth` of the `Layout/Tab` cop.

# Examples

```ruby
# bad
if condition
  do_stuff(bar)
end

unless qux.empty?
  Foo.do_something
end

# good
do_stuff(bar) if condition
Foo.do_something unless qux.empty?
```

[Source](http://www.rubydoc.info/gems/rubocop/RuboCop/Cop/Style/IfUnlessModifier)