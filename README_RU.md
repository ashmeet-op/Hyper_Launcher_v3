<H1 align="center">Hyper Launcher 3</H1>


<p align="center">
  <img src="https://yt3.googleusercontent.com/RC9iOqHVK1Q6Cun4MsxPt1D0TNWVM-8dPgdlCekFq7werQ3Uxm7H0VUz4yqho1-zGBn4-JfU=s160-c-k-c0x00ffffff-no-rj" width="150" height="150" alt="Hyper Launcher logo"><br><br>
  <a href="https://crowdin.com/project/pojavlauncher"><img src="https://badges.crowdin.net/pojavlauncher/localized.svg" alt="Crowdin"></a>
  <a href="https://discord.gg/UR5Wfage"><img src="https://img.shields.io/discord/1365346109131722753.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2" alt="Discord"></a>
</p>

---

## Навигация
- [О проекте](#о-проекте)
- [Возможности](#возможности)
- [Поддерживаемые загрузчики модов](#поддерживаемые-загрузчики-модов)
- [Как получить Hyper Launcher](#как-получить-hyper-launcher)
- [Сборка](#сборка)
- [Текущий план развития](#текущий-план-развития)
- [Известные проблемы](#известные-проблемы)
- [Лицензия](#лицензия)
- [Как внести вклад](#как-внести-вклад)
- [Благодарности и сторонние компоненты](#благодарности-и-сторонние-компоненты-и-их-лицензии-если-доступны)

---

## О проекте

Hyper Launcher 3 — быстрый и настраиваемый лаунчер Minecraft: Java Edition для Android, форк [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher), в основе которого лежит [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher). Он также включает встроенную поддержку Rust edition на базе [horizon-rust/horizon](https://github.com/horizon-rust/horizon), используя в качестве конкретной реализации форк [starling-client/starling (Релиз v0.7.7)](https://github.com/starling-client/starling/releases#release-v0.7.7).

Он способен запускать практически любую версию Minecraft — от `rd-132211` до последних снапшотов (включая версии Combat Test) — и поддерживает такие загрузчики модов, как [Forge](https://files.minecraftforge.net/), [Fabric](http://fabricmc.net/), NeoForge и Quilt, а также моды вроде [OptiFine](https://optifine.net).

Hyper Launcher создан для того, чтобы обеспечить:
- Плавный геймплей Minecraft Java на Android
- Современный дизайн Material UI 3
- Более удобную работу с модпаками

---

## Возможности

- Запуск версий Minecraft от `rd-132211` до последних снапшотов
- Поддержка Fabric, Forge, NeoForge и Quilt
- Импорт модпаков (`.mrpack` / архивы CurseForge)
- Система управления инстансами
- Поддержка пользовательских сборок Java
- Встроенное управление аккаунтами
- Выбор рендерера
- Настройки производительности
- Встроенные инструменты для моддинга
- Поддержка Mobile Glues
- Поддержка Rust edition (на базе [horizon-rust/horizon](https://github.com/horizon-rust/horizon) через форк [starling-client/starling](https://github.com/starling-client/starling))

---

## Поддерживаемые загрузчики модов

- Fabric
- Forge
- NeoForge
- Quilt
- LiteLoader *(экспериментально)*

---

## Как получить Hyper Launcher

Получить Hyper Launcher 3 можно тремя способами:

1. **Автоматические сборки** — получите тестовые сборки из [GitHub Actions](https://github.com/HyperLauncher/HyperLauncher/actions).

2. **Google Play** — скоро.

3. **Сборка из исходного кода** — см. раздел [Сборка](#сборка) ниже.

---

## Сборка

Соберите лаунчер (все необходимые компоненты будут загружены автоматически):

```bash
./gradlew :Hyper_Launcher_v3:assembleDebug
```

> Замените `./gradlew` на `.\gradlew.bat`, если сборка выполняется в Windows.

---

## Текущий план развития

- [x] Система инстансов вместо профилей
- [x] Поддержка версии 1.21.5 "из коробки"
- [x] Импорт mrpack/архивов CurseForge
- [x] Современный интерфейс Material 3 Expressive
- [x] Менеджер модов
- [ ] Улучшенная поддержка контроллеров
- [ ] Расширенная совместимость рендереров
- [ ] Улучшения менеджера загрузок
- [ ] Импорт инстансов, совместимых с MMC
- [ ] Замена Holy-GL4ES 1.1.5 на KW (возможно? нужно разобраться с требованиями)

---

## Известные проблемы

- У некоторых физических мышей может наблюдаться очень низкая скорость курсора
- На Holy GL4ES крупные текстурные атласы могут искажаться (растянутые/блочные текстуры в модпаках)
- Наверняка есть и другие — для этого у нас есть баг-трекер 😉

---

## Лицензия

Hyper Launcher 3 распространяется под лицензией [GNU LGPLv3](LICENSE).

---

## Как внести вклад

Мы рады любому вкладу! Приветствуется не только код — вы можете помочь с вики или поучаствовать в [переводе](https://crowdin.com/project/pojavlauncher).

Любые изменения кода следует оформлять в виде pull request. В описании нужно объяснить, что делает код, и указать шаги для его запуска.

---

## Благодарности и сторонние компоненты (и их лицензии, если доступны)

Hyper Launcher 3 — форк [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher), который, в свою очередь, основан на [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher). Вся заслуга принадлежит обеим командам и всем участникам разработки выше по цепочке.

- [horizon-rust/horizon](https://github.com/horizon-rust/horizon): Движок/бэкенд Rust edition
- [starling-client/starling](https://github.com/starling-client/starling): Используемый форк для поддержки Rust edition в проекте
- [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher): [лицензия GNU LGPLv3](https://github.com/MojoLauncher/MojoLauncher/blob/v3_openjdk/LICENSE)
- [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher): [лицензия GNU LGPLv3](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE)
- [Boardwalk](https://github.com/zhuowei/Boardwalk) (JVM Launcher): лицензия неизвестна / [Apache License 2.0](https://github.com/zhuowei/Boardwalk/blob/master/LICENSE) or GNU GPLv2
- Android Support Libraries: [Apache License 2.0](https://android.googlesource.com/platform/prebuilts/maven_repo/android/+/master/NOTICE.txt)
- [GL4ES](https://github.com/PojavLauncherTeam/gl4es): [лицензия MIT](https://github.com/ptitSeb/gl4es/blob/master/LICENSE)
- [OpenJDK](https://github.com/PojavLauncherTeam/openjdk-multiarch-jdk8u): [лицензия GNU GPLv2](https://openjdk.java.net/legal/gplv2+ce.html)
- [LWJGL3](https://github.com/MojoLauncher/lwjgl3): [лицензия BSD-3](https://github.com/LWJGL/lwjgl3/blob/master/LICENSE.md)
- [Mesa 3D Graphics Library](https://gitlab.freedesktop.org/mesa/mesa): [лицензия MIT](https://docs.mesa3d.org/license.html)
- [pro-grade](https://github.com/pro-grade/pro-grade) (менеджер безопасности для песочницы Java): [Apache License 2.0](https://github.com/pro-grade/pro-grade/blob/master/LICENSE.txt)
- [bhook](https://github.com/bytedance/bhook) (используется для перехвата кодов завершения): [лицензия MIT](https://github.com/bytedance/bhook/blob/main/LICENSE)
- [Authlib-Injector](https://github.com/yushijinhun/authlib-injector) (используется для авторизации через ely.by): [AGPL-3.0](https://github.com/yushijinhun/authlib-injector/blob/develop/LICENSE)
- [alsoft](https://github.com/kcat/openal-soft/) (библиотека вывода звука): [GNU LGPL](https://github.com/kcat/openal-soft/blob/master/COPYING) и [изменённый PFFFT](https://github.com/kcat/openal-soft/blob/master/LICENSE-pffft)
- [oboe](https://github.com/google/oboe): [Apache License 2.0](https://github.com/google/oboe/blob/main/LICENSE)
- [exp4j](https://github.com/fasseg/exp4j): [лицензия Apache 2.0](https://github.com/fasseg/exp4j/blob/master/LICENSE.txt)
- [Gson](https://github.com/google/gson): [лицензия Apache 2.0](https://github.com/google/gson/blob/master/LICENSE)
- [Shaderc](https://github.com/google/shaderc) и [SPIRV-Cross](https://github.com/KhronosGroup/SPIRV-Cross): [лицензия Apache 2.0](https://github.com/google/shaderc/blob/main/LICENSE)
- Благодарим [Mineskin](https://mineskin.eu/) за предоставление аватаров Minecraft.
