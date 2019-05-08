# Release Notes

### [v5.0.0](https://github.com/MSF-Jarvis/viscerion/releases/5.0.0)
- Add an adaptive icon and an alternative concept icon
- Add live log viewer
- Ensure global exclusions are not exported with configs to prevent import bugs
- Support Magisk 19.0 and above
- Improve and speed up exclusion picker
- Improve landscape layout (Fixes Android TV not seeing all create options)
- Fix bug which caused excluded applications count to be incorrect
- Fix bug which caused settings to crash on first run
- Make app theme follow system night mode

### [v4.0.0](https://github.com/MSF-Jarvis/viscerion/releases/4.0.0)

- Add support for Magisk 18.2
- Fix lingering theming issue with bottom sheets
- Add support for Tasker
- Automatically export configurations to external storage when upgrading
- Use a Google Messages style FAB in tunnel list
- Tweak dialog UI
- Add translations for Portuguese Brazilian and Russian
- Move theme toggle to overflow menu for easier access
- Improved speeds for ARM devices on userspace backend

### [v3.1.1](https://github.com/MSF-Jarvis/viscerion/releases/3.1.1)

- Update userspace backend to latest upstream, should bring significant
  speed improvements.
- Fix more theming related issues.


### [v3.1.0](https://github.com/MSF-Jarvis/viscerion/releases/3.1.0)

- Rewrite theming options - Replace light/dark/black options
  with a choice between light and black.
- Redesign settings screen with categories
- Resolve issues with installation of Magisk module on rooted
  mode.
- Code improvements and dependency updates.


### [v3.0.0](https://github.com/MSF-Jarvis/viscerion/releases/3.0.0)

- Initial release with Viscerion branding
- Allow switching between whitelist and blacklist for userspace backend (more below)
- Smaller install size
- Ensure notification reacts to tunnel changes from command line tools
- Disable sound and notification for wg-quick notification
- Fix some misleading error messages
- Ensure keyboard shows up for tunnel name when importing QR codes
- Faster layout load times
- Code cleanup across the board

When the whitelist option is enabled, all apps that are not in the exclusions
list are exempt from going through the tunnel, and the ones in the list are
the only ones which use the tunnel for networking.


### [v2.0.0](https://github.com/MSF-Jarvis/viscerion/releases/2.0.0)

- Rewrite entire codebase based on upstream remodel
- Future-proof Magisk version detection
- Fix restorecon issues with Magisk
- Ensure command line tools know the correct paths to probe for config files
- Update userspace implementation to work with Android 9's bionic restrictions
