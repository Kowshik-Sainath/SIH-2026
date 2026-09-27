package com.example.thasmathjagratha.repository

class MockTranslationService {

    /**
     * Translates emergency & distress messages into any of the 10 supported Indian / English languages.
     */
    fun translate(text: String, targetLanguage: String): String {
        if (text.isBlank()) return ""

        val trimmed = text.trim()

        // Flood / Water level templates
        if (trimmed.contains("flood", ignoreCase = true) || trimmed.contains("water", ignoreCase = true) || trimmed.contains("వరద", ignoreCase = true) || trimmed.contains("बाढ़", ignoreCase = true)) {
            return when (targetLanguage) {
                "Telugu" -> "విజయవాడలో వరద నీరు వేగంగా పెరుగుతోంది. సురక్షిత ప్రాంతాలకు వెళ్లండి."
                "Hindi" -> "विजयवाड़ा में बाढ़ का पानी तेजी से बढ़ रहा है। सुरक्षित स्थानों पर जाएं।"
                "Tamil" -> "விஜயவாடாவில் வெள்ள நீர் வேகமாக உயர்கிறது. பாதுகாப்பான இடத்திற்கு செல்லவும்."
                "Kannada" -> "ವಿಜಯವಾಡದಲ್ಲಿ ಪ್ರವಾಹ ನೀರು ವೇಗವಾಗಿ ಏರುತ್ತಿದೆ. ಸುರಕ್ಷಿತ ಸ್ಥಳಕ್ಕೆ ತೆರಳಿ."
                "Malayalam" -> "വിജയവാഡയിൽ വെള്ളപ്പൊക്ക ജലനിരപ്പ് വേഗത്തിൽ ഉയരുന്നു. സുരക്ഷിത സ്ഥാനത്തേക്ക് മാറുക."
                "Marathi" -> "विजयवाड्यात पुराचे पाणी वेगाने वाढत आहे. सुरक्षित ठिकाणी जा."
                "Bengali" -> "বিজয়ওয়াড়ায় বন্যার জল দ্রুত বৃদ্ধি পাচ্ছে। নিরাপদ স্থানে চলে যান।"
                "Gujarati" -> "વિજયવાડામાં પૂરના પાણી ઝડપથી વધી રહ્યા છે. સુરક્ષિત સ્થળે ખસી જાઓ."
                "Punjabi" -> "ਵਿਜੇਵਾੜਾ ਵਿੱਚ ਹੜ੍ਹ ਦਾ ਪਾਣੀ ਤੇਜ਼ੀ ਨਾਲ ਵਧ ਰਿਹਾ ਹੈ। ਸੁਰੱਖਿਅਤ ਥਾਂ 'ਤੇ ਜਾਓ।"
                "Odia" -> "ବିଜୟୱାଡାରେ ବନ୍ୟା ଜଳ ଦ୍ରୁତ ଗତିରେ ବୃଦ୍ଧି ପାଉଛି। ସୁରକ୍ଷିତ ସ୍ଥାନକୁ ଚାଲିଯାଅ।"
                else -> "Flood water levels rising rapidly in Vijayawada. Evacuate to safe areas immediately."
            }
        }

        // Rescue / Evacuation templates
        if (trimmed.contains("evacuat", ignoreCase = true) || trimmed.contains("rescue", ignoreCase = true) || trimmed.contains("సురక్షిత", ignoreCase = true) || trimmed.contains("बचाव", ignoreCase = true)) {
            return when (targetLanguage) {
                "Telugu" -> "రక్షణ బృందాలు బయలుదేరాయి. తక్కువ ప్రాంతాల నుండి వెంటనే సురక్షిత ప్రాంతాలకు వెళ్లండి."
                "Hindi" -> "बचाव दल रवाना हो चुके हैं। तुरंत निचले इलाकों से सुरक्षित स्थानों पर चले जाएं।"
                "Tamil" -> "மீட்புக் குழுவினர் விரைந்துள்ளனர். உடனடியாக பாதுகாப்பான இடத்திற்கு செல்லவும்."
                "Kannada" -> "ರಕ್ಷಣಾ ಪಡೆಗಳು ಧಾವಿಸಿವೆ. ತಕ್ಷಣ ಸುರಕ್ಷಿತ ಸ್ಥಳಕ್ಕೆ ತೆರಳಿ."
                "Malayalam" -> "രക്ഷാസേന പുറപ്പെട്ടു കഴിഞ്ഞു. ഉടൻ സുരക്ഷിത സ്ഥാനത്തേക്ക് മാറുക."
                "Marathi" -> "बचाव पथके रवाना झाली आहेत. सखल भागातून त्वरित सुरक्षित ठिकाणी जा."
                "Bengali" -> "উদ্ধারকারী দল রওনা হয়েছে। অবিলম্বে নিরাপদ স্থানে চলে যান।"
                "Gujarati" -> "બચાવ ટુકડીઓ રવાના થઈ ગઈ છે. નીચાણવાળા વિસ્તારોમાંથી સુરક્ષિત સ્થળે ખસી જાઓ."
                "Punjabi" -> "ਬਚਾਅ ਟੀਮਾਂ ਰਵਾਨਾ ਹੋ ਚੁੱਕੀਆਂ ਹਨ। ਤੁਰੰਤ ਸੁਰੱਖਿਅਤ ਥਾਂ 'ਤੇ ਜਾਓ।"
                "Odia" -> "ଉଦ୍ଧାରକାରୀ ଦଳ ରୱାନା ହୋଇଛନ୍ତି। ସୁରକ୍ଷିତ ସ୍ଥାନକୁ ଚାଲିଯାଅ।"
                else -> "Rescue teams dispatched. Move away from low-lying areas to safe evacuation shelters immediately."
            }
        }

        // Medical / Assistance templates
        if (trimmed.contains("medical", ignoreCase = true) || trimmed.contains("hospital", ignoreCase = true) || trimmed.contains("వైద్య", ignoreCase = true) || trimmed.contains("चिकित्सा", ignoreCase = true)) {
            return when (targetLanguage) {
                "Telugu" -> "అత్యవసర వైద్య సహాయం అందుబాటులో ఉంది. సమీప ఆసుపత్రికి నివేదించండి."
                "Hindi" -> "आपातकालीन चिकित्सा सहायता उपलब्ध है। निकटतम अस्पताल को रिपोर्ट करें।"
                "Tamil" -> "அவசர மருத்துவ உதவி கிடைக்கிறது. அருகிலுள்ள மருத்துவமனைக்கு செல்லவும்."
                "Kannada" -> "ತುರ್ತು ವೈದ್ಯಕೀಯ ನೆರವು ಲಭ್ಯವಿದೆ. ಹತ್ತಿರದ ಆಸ್ಪತ್ರೆಗೆ ವರದಿ ಮಾಡಿ."
                "Malayalam" -> "അടിയന്തര വൈദ്യസഹായം ലഭ്യമാണ്. അടുത്തുള്ള ആശുപത്രിയിൽ ബന്ധപ്പെടുക."
                "Marathi" -> "तातडीची वैद्यकीय मदत उपलब्ध आहे. जवळच्या रुग्णालयात संपर्क साधा."
                "Bengali" -> "জরুরী চিকিৎসা সহায়তা উপলব্ধ। নিকটস্থ হাসপাতালে যোগাযোগ করুন।"
                "Gujarati" -> "ઇમરજન્સી તબીબી સહાય ઉપલબ્ધ છે. નજીકની હોસ્પિટલમાં સંપર્ક કરો."
                "Punjabi" -> "ਐਮਰਜੈਂਸੀ ਡਾਕਟਰੀ ਮਦਦ ਉਪਲਬਧ ਹੈ। ਨੇੜਲੇ ਹਸਪਤਾਲ ਵਿੱਚ ਸੰਪਰਕ ਕਰੋ।"
                "Odia" -> "ଜରୁରୀ ଚିକିତ୍ସା ସହାୟତା ଉପଲବ୍ଧ। ନିକଟସ୍ଥ ଡାକ୍ତରଖାନାରେ ଯୋଗାଯୋଗ କରନ୍ତୁ।"
                else -> "Emergency medical assistance available. Report to the nearest health center immediately."
            }
        }

        // Default phrase translation generator
        val suffix = when (targetLanguage) {
            "Telugu" -> " (తెలుగు అనువాదం)"
            "Hindi" -> " (हिंदी अनुवाद)"
            "Tamil" -> " (தமிழ் மொழிபெயர்ப்பு)"
            "Kannada" -> " (ಕನ್ನಡ ಅನುವಾದ)"
            "Malayalam" -> " (മലയാളം തർജ്ജമ)"
            "Marathi" -> " (मराठी भाषांतर)"
            "Bengali" -> " (বাংলা অনুবাদ)"
            "Gujarati" -> " (ગુજરાતી અનુવાદ)"
            "Punjabi" -> " (ਪੰਜਾਬੀ ਅਨੁਵਾਦ)"
            "Odia" -> " (ଓଡ଼ିଆ ଅନୁବାଦ)"
            else -> " (English)"
        }

        return "$text$suffix"
    }
}
