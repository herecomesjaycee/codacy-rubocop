
This cop checks whether some constant value isn't a
mutable literal (e.g. array or hash).

# Examples

```ruby
# bad
CONST = [1, 2, 3]

# good
CONST = [1, 2, 3].freeze

# good
CONST = <<~TESTING.freeze
This is a heredoc
TESTING
```

[Source](http://www.rubydoc.info/gems/rubocop/RuboCop/Cop/Style/MutableConstant)