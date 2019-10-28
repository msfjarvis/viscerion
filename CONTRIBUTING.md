# Contributing guidelines

Viscerion welcomes contributions in all shapes and forms from anybody willing to do so :)

### Contributing documentation

Viscerion has very little in the way of documentation, save for a lone wiki page on the [Tasker integration](https://github.com/msfjarvis/viscerion/wiki/Tasker-integration). I'd love for this to change and evolve into more in-depth documentation for the various features Viscerion adds over the official Android client as well as basic WireGuard usage, since it seems to be quite lacking as well.

### Contributing code

The bulk of Viscerion is split into four components

- [The application codebase](app/src/main)
- [wg(8) and wg-quick(8)](native/tools)
- [libwg-go](native/tools/libwg-go)
- [Java crypto routines](crypto/src/main)

`wg(8)`, and `wg-quick(8)` are developed upstream at ZX2C4's [Git repository](https://git.zx2c4.com/WireGuard), and as such all improvements should be contributed there first, and then you can start an issue on this repo to let me know to pull the updates with your contributions.

`libwg-go` is a thin Golang wrapper around the main [wireguard-go](https://git.zx2c4.com/wireguard-go) library and you will most likely want to contribute to `wireguard-go` and not the wrapper in this repository. As with `wg(8)` and `wg-quick(8)`, land your changes upstream, and then let me know and I will pull the update in Viscerion.

The Java crypto subproject in this repository is also an [upstream component](https://git.zx2c4.com/wireguard-android/tree/app/src/main/java/com/wireguard/crypto/), and contributions should be submitted to upstream. As with other upstream components, I will timely pull down updates into Viscerion.

Everything not covered above belongs directly to this repository and welcomes contributions in all forms and sizes. The only requirement here is that you follow the code style of this repository. I will follow up with every PR to its completion and help along the way, so don't worry too much about making mistakes :)


It is recommended that you install the Git pre-push hook to ensure all code being pushed builds and adheres to the code style guidelines.

```bash
ln -sf $(pwd)/config/pre-push-recommended.sh .git/hooks/pre-push
```
