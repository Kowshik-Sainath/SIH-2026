# Offline TTS

`TtsDemoScreen` is reachable from the Speak screen. Download a pack once while online; `TtsEngine.init()` and `synthesize()` only use files in app storage and run inference through the sherpa-onnx 1.13.8 Android AAR. The existing STT placeholder bindings were moved into the app's `stt.legacy` package to avoid class-name collisions with the real AAR. This does not upgrade STT inference; the prior STT behavior is preserved.

## Device-to-device alerts

`MainActivity` starts `LocalAlertTransport` while the app process is running. Publishing a government alert or submitting an emergency report broadcasts a versioned JSON packet on the current Wi-Fi subnet (UDP port 28154), and also sends it to bonded Bluetooth Classic devices running this app when Bluetooth permission is granted. The receiving app deduplicates packet IDs, adds the alert to its repository, and automatically runs `TtsEngine` then `TtsAudioPlayer` using the packet's language. The voice pack **must already be installed on the receiving phone**. A missing or unsupported pack leaves a visible alert and a message asking for that pack; receiving an alert never starts a network model download.

The sender must enter the alert text in the language selected for the alert. This app's translation service is a demo template, so the wire protocol carries the original text and selected language; it does not claim to translate it. A receiver speaks the exact text it gets with that language's voice. The network channel currently has no pairing secret or sender authentication: received alerts are marked unverified. UDP broadcast can be blocked by router client isolation and does not provide delivery confirmation. Bluetooth requires prior OS pairing and `BLUETOOTH_CONNECT` permission. Reception is tied to the app process; this is not yet a foreground service that can survive process death. Those limitations matter before using it as a real emergency alert system.

Outgoing alerts use a durable outbox at `files/transport/alert-outbox.json`. The packet is saved before transmission, retried every five seconds while the app process is alive, and retried again after the app is opened after a process restart. A queued packet is removed only after at least one local Wi-Fi broadcast or Bluetooth RFCOMM connection is made. If there is no usable local interface, the sender UI reports that the alert is queued and will retry automatically. Wi-Fi broadcast is inherently best-effort when a network is present (a UDP broadcast cannot prove that another app instance received it); Bluetooth uses an actual RFCOMM connection. Keep the transport running from the app or service lifecycle if alerts must be delivered while the UI is not visible.

## Voices and licensing

| TTS language | Model group | Default speaker | License |
| --- | --- | ---: | --- |
| Bengali (`bn`) | `vits_rasa_13` | BEN_F = 2 | CC BY 4.0 |
| Kannada (`kn`) | `vits_rasa_13` | KAN_F = 8 | CC BY 4.0 |
| Malayalam (`ml`) | `vits_rasa_13` | MAL_F = 11 | CC BY 4.0 |
| Marathi (`mr`) | `vits_rasa_13` | MAR_F = 12 | CC BY 4.0 |
| Tamil (`ta`) | `vits_rasa_13` | TAM_F = 18 | CC BY 4.0 |
| Telugu (`te`) | `vits_rasa_13` | TEL_F = 19 | CC BY 4.0 |
| Hindi (`hi`) | `hi_IN-rohan-medium` | 0 | Piper voice repository: MIT; check its [model card](https://huggingface.co/rhasspy/piper-voices/blob/main/hi/hi_IN/rohan/medium/MODEL_CARD) for training-data terms |
| English (`en`) | `en_US-lessac-medium` | 0 | Piper voice repository: MIT; check its [model card](https://huggingface.co/rhasspy/piper-voices/blob/main/en/en_US/lessac/medium/MODEL_CARD) for training-data terms |

Rasa downloads one `model.onnx` plus `tokens.txt` for all six languages. There is no additional config or language-ID file in [the ONNX export](https://huggingface.co/MatiasLin/sherpa-onnx-vits-rasa-13/tree/main); the language is selected through the verified speaker ID and native-script input text. [AI4Bharat's model card](https://huggingface.co/ai4bharat/vits_rasa_13) documents those speaker IDs and the `NEWS=10` and `CONV=4` style IDs. The engine passes `emotion_id` through sherpa-onnx's `GenerationConfig.extra`; this requires a build with [emotion input support](https://github.com/k2-fsa/sherpa-onnx/pull/3849), which was merged before 1.13.8. The export metadata lists 1024 possible IDs, but only the documented language speaker IDs are selected by default.

Piper downloads the official `.onnx` and `.onnx.json` pair. [sherpa-onnx's Piper conversion guide](https://github.com/k2-fsa/sherpa/blob/master/docs/source/onnx/tts/piper.rst) requires `tokens.txt`, eSpeak data, and ONNX metadata; the pack manager generates the tokens and appends the metadata after download, then installs one shared `espeak-ng-data` directory. The JSON remains alongside the model for attribution and repeatability. The AAR uses eSpeak phonemization for Piper. Rasa uses its character frontend. Gujarati and Odia are intentionally absent from the TTS list; callers get an explicit unsupported-language error.

## Playback and performance

`TtsEngine` holds at most one `OfflineTts` instance, reuses it across the six Rasa languages, and releases it when switching to a Piper voice. `TtsAudioPlayer` uses `AudioTrack` on an IO thread. Normal playback uses media audio focus and can be stopped. Alert playback requests exclusive transient focus, uses the alarm stream at its maximum setting, and exposes only `acknowledgeAlert()` as an in-app stop control; the previous alarm volume is restored afterward. Android system policy, Do Not Disturb, physical volume controls, and competing system alarms can still override or limit playback. Piper has no emotion ID: its alert delivery uses this playback path and a small speed increase, not a separate voice style. Rasa alert uses NEWS and normal uses CONV; the perceived urgency of either style must be evaluated with listeners before emergency deployment.

Model load time, synthesis time, file-size-based approximate model memory, and real-time factor are logged in `TtsTelemetry` and shown beside STT diagnostics in the Speak screen. The model file size is not a measured process RSS value. Synthesis and playback stay off the UI thread. No additional quantization is applied.

Text is trimmed and whitespace normalized. Rasa numerals are expanded digit by digit in the target language; Piper/eSpeak handles its own numerals. Rasa trailing punctuation is removed because the [export card](https://huggingface.co/MatiasLin/sherpa-onnx-vits-rasa-13) reports it can produce a noisy extra fragment. Multi-digit numbers are currently spoken as individual digits, so expand quantities and addresses upstream where context matters. Local abbreviations, place-name pronunciation, and Hindi/English code mixing are not solved by these models; send already normalized target-language text when accurate pronunciation matters. In particular, English words inside Hindi text may be mispronounced by the Hindi Piper eSpeak voice. The demo does not claim reliable emergency intelligibility without device and listener testing.

Example integration:

```kotlin
val engine = TtsEngine(context)
engine.packManager.download("te") // one download makes bn/kn/ml/mr/ta/te available
engine.init("te")
val player = TtsAudioPlayer(context)
player.play(engine.synthesize(translatedVoiceNote, TtsMode.NORMAL))
player.play(engine.synthesize(translatedAlert, TtsMode.ALERT))
// Your alert acknowledgement UI calls player.acknowledgeAlert().
engine.destroy()
```
