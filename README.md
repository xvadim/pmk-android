# "MK 61/54" для платформы Android

Здесь размещен исходный текст приложения "МК 61/54" для платформы Android - эмулятора программируемых калькуляторов
(ПМК) "Электроника МК 61", "Электроника МК-54" и других совместимых с ними советских калькуляторов расширяющегося ряда.
Данный форк содержит немного улучшенный вид для МК-54 и возможность экспортировать/импортировать программы в/из 
внешних файлов.

<a href="https://play.google.com/store/apps/details?id=com.cax.pmk.ext&hl=ru" alt="Download from Google Play">
  <img src="http://www.android.com/images/brand/android_app_on_play_large.png">
</a>

## Лицензия

* [GNU General Public License v.3.0](http://www.gnu.org/licenses/gpl-3.0.html)


## Справка

"МК-61" - самый популярный представитель ряда программируемых калькуляторов (Б3-34, МК-54, МК-56, МК-61, МК-52), давших поколению 1980-х возможность иметь собственный мини-компьютер и запускать на нём игры, популяризованные журналами "Техника-молодёжи" и "Наука и жизнь".

В отличие от симуляторов, имитирующих поведение калькулятора лишь приблизительно, данный эмулятор реализует совместимость на уровне микрокода и ведёт себя точь-в-точь как настоящиe МК-61/MK-54, полностью повторяя в том числе и недокументированные возможности, и невысокую точность вычислений.

Механизм эмуляции реализован на основе исходного кода Феликса Лазарева ([http://code.google.com/p/emu145](проект emu145)) и портирован с C++ на Java.
Скорость эмуляции значительно улучшена по сравнению с оригинальной, что позволяет эмулировать МК-61 в реальном времени на любом телефоне или планшете.

Для удобства приложение предоставляет возможность сохранения и загрузки состояний эмуляции, что приближает эмулятор по
возможностям к калькулятору МК-52.
Также имеется возможность экспорта/импорта состояния в/из внешних файлов.

[Инструкция к калькулятору](http://htmlpreview.github.io/?instruction/instruction.html)

## Сборка эмулятора

Для самостоятельной сборки приложения необходимо установить [Android SDK](http://developer.android.com/sdk/index.html)
и импортировать папку pmk следующим образом: "File" -> "Import" -> "Android" -> "Existing Android Code into Workspace".


## Участие в разработке

Вы можете предлагать свои исправления и дополнения эмулятора, используя GitHub fork и
[pull requests](https://github.com/github/android/pulls).


## Контакты

Вопросы и пожелания, касающиеся работы эмулятора, направляйте по адресу: <vadim.khohlov@gmail.com>.

---

# "MK 61/54" for Android

This repository contains the source code for "MK 61/54" Android application - emulator of Soviet RPN programmable
calculators "Electronika MK 61" and "Electronika MK-54".
This fork allows to export/import programs to/from external files.

<a href="https://play.google.com/store/apps/details?id=com.cax.pmk" alt="Download from Google Play">
  <img src="http://www.android.com/images/brand/android_app_on_play_large.png">
</a>


## License

* [GNU General Public License v.3.0](http://www.gnu.org/licenses/gpl-3.0.html)


## Description

MK-61 was the best seller of all USSR programmable calculators of 1980-s (B3-34, MK-54, MK-56, MK-61, MK-52).

Calculators are emulated on microcode level so they behave exactly like the original devices, including all non-documented features and inaccurate calculations.

This app's emulation engine Java code is based on C++ source of Felix Lazarev's ([http://code.google.com/p/emu145](emu145 project)).
Emulation was highly optimized for speed and should run in real time on any Android phone or tablet.

For ease of use application features saving and loading emulation states. Also it is possible to export/import states 
to/from external files.


## Building

The build requires [Android SDK](http://developer.android.com/sdk/index.html) to be installed in your development environment.


## Contributing

Please fork this repository and contribute back using [pull requests](https://github.com/github/android/pulls).


## Contacts

Feel free to send all your questions and suggestions about emulator to e-mail <vadim.khohlov@gmail.com>.
