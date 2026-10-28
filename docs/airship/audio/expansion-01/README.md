# Реплики расширения — пакет 01

Ровно 10 новых реплик, все восемь ранее выбранных голосов. Капитан и Мара по две; остальные по одной.

[Прослушать весь пакет](batch-01-preview.mp3). Таймкоды приблизительные из-за округления и перекодирования.

| № | Начало | Реплика | Голос | Файл |
|---|---|---|---|---|
| 1 | 00:00 | captain_request | voice-01 | [MP3](captain_request.mp3) |
| 2 | 00:11 | captain_thanks | voice-01 | [MP3](captain_thanks.mp3) |
| 3 | 00:24 | mara_request | voice-03 | [MP3](mara_request.mp3) |
| 4 | 00:33 | mara_thanks | voice-03 | [MP3](mara_thanks.mp3) |
| 5 | 00:43 | quartermaster_request | voice-05 | [MP3](bram_request.mp3) |
| 6 | 00:54 | oren_request | voice-08 | [MP3](oren_request.mp3) |
| 7 | 01:05 | veya_request | voice-10 | [MP3](veya_request.mp3) |
| 8 | 01:15 | selin_request | voice-12 | [MP3](selin_request.mp3) |
| 9 | 01:26 | lysander_request | voice-14 | [MP3](lysander_request.mp3) |
| 10 | 01:37 | tik_request | voice-18 | [MP3](tik_request.mp3) |

Точный текст и измеренная длительность: `manifest.json`. Игровой манифест: `assets/mmorpg/airship/adventure-voice.json`. Каждый исходник отдельно конвертирован в mono Vorbis OGG и зарегистрирован как streaming sound.

В коде озвучка запускается после подтверждённого сервером изменения принятия/завершения задания; закрытие окна останавливает звук. Если реплика не записана, она остаётся текстовой. Аудио с несовпадающим текстом не проигрывается. Это не озвучка всей кампании.

Проверка структуры и текстов: `python tools/airship/check_adventure_audio.py` — PASS (10/10). Новый клиентский playback и новые unit-тесты в этом проходе ещё не запускались. Исторический успешный CI из общего статуса относится к предыдущему слою, не к этим изменениям.
