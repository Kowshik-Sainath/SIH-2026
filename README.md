# Thasmath Jagratha - Offline Voice-Relay System (SIH Problem Statement 26173)

An offline, on-device voice-relay and emergency alert system developed for Android (Kotlin / Jetpack Compose). Built for disconnected, disaster-response, and infrastructure-free environments where cellular towers, internet backhauls, and cloud services are offline or destroyed.

---

## 1. System Architecture

The end-to-end pipeline operates **100% on-device** without any network calls for inference:

```
[User Audio Input]
        │
        ▼ (16kHz 16-bit Mono PCM)
[Stage 1: RMS / Energy VAD] (Low-latency pre-filter, threshold 0.015f)
        │
        ▼
[Stage 2: Silero VAD v5 ONNX] (Sherpa-ONNX Vad, 643 KB, min speech 150ms)
        │
        ▼ (Valid Speech Segments)
[Sherpa-ONNX ASR Engine] (OfflineRecognizer / NeMo Conformer CTC INT8)
        │
        ▼ (Transcribed Text)
[WireAlert JSON Serializer] (Compact, versioned payload)
        │
        ▼
[Local Alert Transport]
  ├── Wi-Fi LAN Broadcast (UDP DatagramSocket, Port 28154, Subnet 255.255.255.255)
  └── Bluetooth Classic RFCOMM (SPP UUID: 00001101-0000-1000-8000-00805F9B34FB)
        │
   (Receiving Device)
        │
        ▼
[Duplicate Detection & Cache] (Alert ID + Timestamp hash deduplication)
        │
        ▼
[Sherpa-ONNX Offline TTS]
  ├── Rasa 13 VITS (bn, kn, ml, mr, ta, te) [123 MB ONNX]
  └── Piper VITS (hi, en) [63 MB ONNX]
        │
        ▼ (22.05 kHz PCM Audio)
[AudioTrack Low-Latency Player]
```

---

## 2. Language Support Matrix & True Model Specifications

All models are downloaded on-demand by the user via resumable HTTP chunk streaming (`ModelDownloader`) and stored in the app's sandboxed private storage (`context.filesDir/stt_models/` and `context.filesDir/tts_models/`).

### Speech-to-Text (STT) Models

| Language | Code | Engine / Architecture | Hugging Face Repository | Runtime Files & Real Sizes | License | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Telugu** | `te` | NeMo CTC Conformer INT8 | `nmukthap/vertexvoice-indic` | `model.int8.opt.onnx` (739 KB)<br>`model.int8.opt.onnx.data` (136.5 MB)<br>`vocab.txt` (257 tokens, 2.5 KB) | Apache 2.0 | **Supported** |
| **Hindi** | `hi` | NeMo CTC Conformer INT8 | `nmukthap/vertexvoice-indic` | Shared with Indic family (137.2 MB total) | Apache 2.0 | **Supported** |
| **Tamil** | `ta` | NeMo CTC Conformer INT8 | `nmukthap/vertexvoice-indic` | Shared with Indic family (137.2 MB total) | Apache 2.0 | **Supported** |
| **Kannada** | `kn` | NeMo CTC Conformer INT8 | `nmukthap/vertexvoice-indic` | Shared with Indic family (137.2 MB total) | Apache 2.0 | **Supported** |
| **Malayalam**| `ml` | NeMo CTC Conformer INT8 | `nmukthap/vertexvoice-indic` | Shared with Indic family (137.2 MB total) | Apache 2.0 | **Supported** |
| **Marathi** | `mr` | NeMo CTC Conformer INT8 | `nmukthap/vertexvoice-indic` | Shared with Indic family (137.2 MB total) | Apache 2.0 | **Supported** |
| **Gujarati** | `gu` | NeMo CTC Conformer INT8 | `nmukthap/vertexvoice-indic` | Shared with Indic family (137.2 MB total) | Apache 2.0 | **Supported** |
| **Bengali** | `bn` | NeMo CTC Conformer INT8 | `nmukthap/vertexvoice-indic` | Shared with Indic family (137.2 MB total) | Apache 2.0 | **Supported** |
| **Punjabi** | `pa` | NeMo CTC Conformer INT8 | `nmukthap/vertexvoice-indic` | Shared with Indic family (137.2 MB total) | Apache 2.0 | **Supported** |
| **English** | `en` | NeMo Conformer Small INT8 | `csukuangfj/sherpa-onnx-nemo-ctc-en-conformer-small` | `model.int8.onnx` (46.4 MB)<br>`tokens.txt` (11.6 KB, 1025 tokens) | Apache 2.0 | **Supported** |
| **Odia** | `or` | N/A | None verified | N/A | N/A | **OUT OF SCOPE** (Disabled in UI) |

### Text-to-Speech (TTS) Models

| Language | Code | Engine | Model Checkpoint | Runtime Size | License | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Telugu** | `te` | Sherpa-ONNX VITS | `MatiasLin/sherpa-onnx-vits-rasa-13` | 123.4 MB | CC-BY-4.0 | **Supported** |
| **Kannada** | `kn` | Sherpa-ONNX VITS | `MatiasLin/sherpa-onnx-vits-rasa-13` | Shared (123.4 MB) | CC-BY-4.0 | **Supported** |
| **Malayalam**| `ml` | Sherpa-ONNX VITS | `MatiasLin/sherpa-onnx-vits-rasa-13` | Shared (123.4 MB) | CC-BY-4.0 | **Supported** |
| **Marathi** | `mr` | Sherpa-ONNX VITS | `MatiasLin/sherpa-onnx-vits-rasa-13` | Shared (123.4 MB) | CC-BY-4.0 | **Supported** |
| **Tamil** | `ta` | Sherpa-ONNX VITS | `MatiasLin/sherpa-onnx-vits-rasa-13` | Shared (123.4 MB) | CC-BY-4.0 | **Supported** |
| **Bengali** | `bn` | Sherpa-ONNX VITS | `MatiasLin/sherpa-onnx-vits-rasa-13` | Shared (123.4 MB) | CC-BY-4.0 | **Supported** |
| **Hindi** | `hi` | Piper VITS | `rhasspy/piper-voices` (`hi_IN-rohan-medium`) | 63.2 MB | MIT / IndicTTS | **Supported** |
| **English** | `en` | Piper VITS | `rhasspy/piper-voices` (`en_US-lessac-medium`)| 63.4 MB | MIT / LibriTTS | **Supported** |
| **Gujarati** | `gu` | N/A | Not present in Rasa-13 or Piper | N/A | N/A | **OUT OF SCOPE** (Guard blocked) |
| **Odia** | `or` | N/A | Not present in Rasa-13 or Piper | N/A | N/A | **OUT OF SCOPE** (Disabled in UI) |

### Voice Activity Detection (VAD)

- **Model**: `silero_vad.onnx` (Silero VAD v5)
- **Asset Path**: `app/src/main/assets/silero_vad.onnx`
- **Size**: 643,854 bytes (~629 KB)
- **License**: MIT License (Copyright 2020-present Silero Team)
- **Sherpa-ONNX integration**: Configured via `SileroVadModelConfig(minSpeechDuration = 0.15f)`

---

## 3. Before-and-After Claims Diff

| Feature / Metric | Previous Repository Claim (Before) | Real Verified Implementation (After) |
| :--- | :--- | :--- |
| **STT Model Size** | "8.2 MB ultra-compact quantized model" (fictional) | **137.2 MB** for Indic multilingual (`model.int8.opt.onnx` + external `.data`), **46.4 MB** for English. |
| **STT Inference Engine** | Claimed offline sherpa-onnx, but code secretly called `android.speech.SpeechRecognizer` / Google Cloud. | **100% offline `com.k2fsa.sherpa.onnx.OfflineRecognizer`** running on-device via native JNI `.so` libraries. Zero calls to `android.speech`. |
| **Odia Support** | Claimed Odia STT & TTS were functional. | **Strictly OUT OF SCOPE**. Odia disabled in UI with user-facing explanation (no production on-device ONNX model verified). |
| **Gujarati TTS** | Claimed Gujarati TTS was functional. | **Strictly OUT OF SCOPE**. Blocked with explanatory guard (neither Rasa-13 nor Piper provides Gujarati). |
| **VAD Implementation** | Energy-only thresholding stub. | **Two-Stage VAD**: Stage-1 RMS energy filter + Stage-2 Silero VAD v5 ONNX (629 KB, bundled in assets). |
| **P2P Transport** | Claimed "Multi-hop BLE mesh networking". | **Direct Single-Hop Broadcast**: Wi-Fi LAN UDP broadcast (port 28154) + Bluetooth Classic RFCOMM SPP socket. Outbox disk queue for offline retries. |
| **Fallbacks & Stubs** | `createLocalFallbackPack` synthetic mock text generator. | **All mocks and stub recognizers removed**. Real audio is transcribed or fails with clear diagnostic errors. |
| **Download Pipeline** | Unbuffered `URL.openStream()` without retry or Range support. | **Robust `ModelDownloader`**: 64 KB chunk streaming, HTTP `Range: bytes=X-` resume, atomic rename from `.partial`, 3 retries with backoff. |

---

## 4. Network and Transport Details

- **Wi-Fi Transport**:
  - Broadcast address: `255.255.255.255` on UDP port `28154`.
  - Transmits a compact JSON `WireAlert` payload containing alert ID, sender name, message body, language code, and timestamp.
  - Receiving phones parse packet, deduplicate against recent alert cache, display banner, and trigger offline TTS synthesis.
- **Bluetooth Classic Transport**:
  - Uses Serial Port Profile (SPP) with UUID `00001101-0000-1000-8000-00805F9B34FB`.
  - Streams length-prefixed JSON frames over RFCOMM socket.
- **Internet Permission Justification**:
  - `android.permission.INTERNET` is **strictly required** for:
    1. User-initiated downloading of offline language packs from Hugging Face via Wi-Fi.
    2. Local-area Wi-Fi UDP broadcast socket creation (`DatagramSocket`).
  - At no point during speech recognition or synthesis is any network socket opened.

---

## 5. Roadmap & Future Scope

The following items are explicit future milestones and are **not** claimed as working in the current release:

1. **Multi-Hop BLE Mesh**: Extending local Bluetooth / Wi-Fi direct transport into a multi-hop store-and-forward mesh across >10 intermediate nodes.
2. **On-Device Indic Translation**: Neural Machine Translation (e.g. quantized IndicTrans2) to translate transcribed text across Indic languages before TTS.
3. **Odia Support**: Training or exporting a lightweight CTC Conformer ONNX model and Piper voice checkpoint for Odia (`or`).
4. **Gujarati TTS**: Training and quantizing an open-source Piper voice for Gujarati (`gu`).
