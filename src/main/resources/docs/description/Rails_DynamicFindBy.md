
This cop checks dynamic `find_by_*` methods.
Use `find_by` instead of dynamic method.
See. https://github.com/rubocop-hq/rails-style-guide#find_by

# Examples

```ruby
# bad
User.find_by_name(name)

# bad
User.find_by_name_and_email(name)

# bad
User.find_by_email!(name)

# good
User.find_by(name: name)

# good
User.find_by(name: name, email: email)

# good
User.find_by!(email: email)
```

[Source](http://www.rubydoc.info/gems/rubocop/RuboCop/Cop/Rails/DynamicFindBy)