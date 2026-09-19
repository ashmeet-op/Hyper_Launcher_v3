<H1 align="center">Hyper Launcher 3</H1>


<p align="center">
  <img src="https://yt3.googleusercontent.com/RC9iOqHVK1Q6Cun4MsxPt1D0TNWVM-8dPgdlCekFq7werQ3Uxm7H0VUz4yqho1-zGBn4-JfU=s160-c-k-c0x00ffffff-no-rj" width="150" height="150" alt="Hyper Launcher logo"><br><br>
  <a href="https://crowdin.com/project/pojavlauncher"><img src="https://badges.crowdin.net/pojavlauncher/localized.svg" alt="Crowdin"></a>
  <a href="https://discord.gg/URgJyNER8F"><img src="https://img.shields.io/discord/1365346109131722753.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2" alt="Discord"></a>
</p>

---
  [![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/B0N827ACQL)
## Navigation
- [About](#about)
- [Features](#features)
- [Supported Mod Loaders](#supported-mod-loaders)
- [Getting Hyper Launcher](#getting-hyper-launcher)
- [Building](#building)
- [Current Roadmap](#current-roadmap)
- [Known Issues](#known-issues)
- [License](#license)
- [Contributing](#contributing)
- [Credits & Third Party Components](#credits--third-party-components-and-their-licenses-if-available)

---

## About

Hyper Launcher 3 is a fast and customizable Minecraft: Java Edition launcher for Android, forked from [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher) and ultimately based on [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher).

It can run almost every version of Minecraft — from `rd-132211` to the latest snapshots (including Combat Test versions) — and supports modloaders such as [Forge](https://files.minecraftforge.net/), [Fabric](http://fabricmc.net/), NeoForge, and Quilt, as well as mods like [OptiFine](https://optifine.net).

Hyper Launcher is designed for:
- Smooth Minecraft Java gameplay on Android
- Modern Material UI 3 Design
- Better modpack handling

---

## Features

- Launch Minecraft versions from `rd-132211` to latest snapshots
- Fabric, Forge, NeoForge and Quilt support
- Modpack import support (`.mrpack` / CurseForge zip)
- Instance management system
- Custom Java runtime support
- Integrated account management
- Renderer selection support
- Performance tuning options
- Built-in modding tools
- Mobile Glues support
---

## Supported Mod Loaders

- Fabric
- Forge
- NeoForge
- Quilt
- LiteLoader *(experimental)*

---

## Getting Hyper Launcher

You can get Hyper Launcher 3 via three methods:

1. **Automatic Builds** — Get development builds from [GitHub Actions](https://github.com/HyperLauncher/HyperLauncher/actions).

2. **Google Play** — Coming soon.

3. **Build from Source** — See [Building](#building) below.

---

## Building

Build the launcher (it will automatically download all required components):

```bash
./gradlew :Hyper_Launcher_v3:assembleDebug
```

> Replace `./gradlew` with `.\gradlew.bat` if you are building on Windows.

---

## Current Roadmap

- [x] Instance system in favor of profiles
- [x] Out-of-the-box 1.21.5 support
- [x] mrpack/CurseForge zip import
- [x] Modern Material 3 expressive interface
- [x] Mod manager
- [x] Better controller support
- [x] Enhanced renderer compatibility
- [x] Download manager improvements
- [x] MMC-compatible instance import

---

## Known Issues

- Some physical mice may have very slow mouse speed
- On Holy GL4ES, large texture atlases may be distorted (resulting in stretched/blocky textures in modpacks)
- Probably more — that's why we have a bug tracker 😉

---

## License

Hyper Launcher 3 is licensed under [GNU LGPLv3](LICENSE).

---

## Contributing

Contributions are welcome! We welcome any type of contribution, not only code. For example, you can help the wiki shape up or contribute to the [translation](https://crowdin.com/project/pojavlauncher).

Any code change should be submitted as a pull request. The description should explain what the code does and give steps to execute it.

---

## Credits & Third Party Components and Their Licenses (if available)

Hyper Launcher 3 is a fork of [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher), which is itself based on [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher). Full credit goes to both teams and all upstream contributors.

- [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher): [GNU LGPLv3 License](https://github.com/MojoLauncher/MojoLauncher/blob/v3_openjdk/LICENSE)
- [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher): [GNU LGPLv3 License](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE)
- [Boardwalk](https://github.com/zhuowei/Boardwalk) (JVM Launcher): Unknown License / [Apache License 2.0](https://github.com/zhuowei/Boardwalk/blob/master/LICENSE) or GNU GPLv2
- Android Support Libraries: [Apache License 2.0](https://android.googlesource.com/platform/prebuilts/maven_repo/android/+/master/NOTICE.txt)
- [GL4ES](https://github.com/PojavLauncherTeam/gl4es): [MIT License](https://github.com/ptitSeb/gl4es/blob/master/LICENSE)
- [OpenJDK](https://github.com/PojavLauncherTeam/openjdk-multiarch-jdk8u): [GNU GPLv2 License](https://openjdk.java.net/legal/gplv2+ce.html)
- [LWJGL3](https://github.com/MojoLauncher/lwjgl3): [BSD-3 License](https://github.com/LWJGL/lwjgl3/blob/master/LICENSE.md)
- [Mesa 3D Graphics Library](https://gitlab.freedesktop.org/mesa/mesa): [MIT License](https://docs.mesa3d.org/license.html)
- [pro-grade](https://github.com/pro-grade/pro-grade) (Java sandboxing security manager): [Apache License 2.0](https://github.com/pro-grade/pro-grade/blob/master/LICENSE.txt)
- [bhook](https://github.com/bytedance/bhook) (Used for exit code trapping): [MIT License](https://github.com/bytedance/bhook/blob/main/LICENSE)
- [Authlib-Injector](https://github.com/yushijinhun/authlib-injector) (Used for authorisation via ely.by): [AGPL-3.0](https://github.com/yushijinhun/authlib-injector/blob/develop/LICENSE)
- [alsoft](https://github.com/kcat/openal-soft/) (Audio output library): [GNU LGPL](https://github.com/kcat/openal-soft/blob/master/COPYING) and [modified PFFFT](https://github.com/kcat/openal-soft/blob/master/LICENSE-pffft)
- [oboe](https://github.com/google/oboe): [Apache License 2.0](https://github.com/google/oboe/blob/main/LICENSE)
- [exp4j](https://github.com/fasseg/exp4j): [Apache License 2.0](https://github.com/fasseg/exp4j/blob/master/LICENSE.txt)
- [Gson](https://github.com/google/gson): [Apache License 2.0](https://github.com/google/gson/blob/master/LICENSE)
- [SPIRV-Cross](https://github.com/KhronosGroup/SPIRV-Cross): [Apache License 2.0](https://github.com/google/shaderc/blob/main/LICENSE)
- [translate.js](https://github.com/xnx3/translate) (AI translation service by [zvo.cn](https://zvo.cn)): [MIT License](https://github.com/xnx3/translate/blob/master/LICENSE)
- Thanks to [Mineskin](https://mineskin.eu/) for providing Minecraft avatars.
