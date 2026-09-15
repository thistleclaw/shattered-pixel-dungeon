# Adventure localization format

Adventure Mode uses libGDX `I18NBundle` for its standalone localization bundle.

Parameterized strings must use MessageFormat-style placeholders:

- `{0}` for the first argument
- `{1}` for the second argument
- and so on

Do **not** use printf-style placeholders such as `%s` or `%d`; `I18NBundle.format()` does not substitute them and they will appear literally in the UI.

The Android CI workflows contain a guard which fails the build if `%s` or `%d` appears in the Adventure localization directory.
