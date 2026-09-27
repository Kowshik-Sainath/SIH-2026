package com.example.thasmathjagratha.stt.decoder

/**
 * Provides domain-specific phrase biasing and hotword boosting for emergency, rescue,
 * and distress communications across supported languages.
 */
object DomainPhraseBiasing {

    /**
     * Emergency distress phrases by language code.
     */
    private val emergencyPhrases = mapOf(
        "te" to listOf("సహాయం", "ప్రమాదం", "అత్యవసర", "పోలీస్", "ఆసుపత్రి", "వరద", "రక్షణ", "డాక్టర్", "అగ్ని", "ఆపద"),
        "hi" to listOf("मदद", "दुर्घटना", "आपातकाल", "पुलिस", "अस्पताल", "बाढ़", "बचाव", "डॉक्टर", "आग", "खतरा"),
        "en" to listOf("help", "accident", "emergency", "police", "hospital", "flood", "rescue", "doctor", "fire", "danger", "evacuate", "ambulance"),
        "ta" to listOf("உதவி", "விபத்து", "அவசரம்", "காவல்துறை", "மருத்துவமனை", "வெள்ளம்", "மீட்பு", "தீ"),
        "kn" to listOf("ಸಹಾಯ", "ಅಪಘಾತ", "ತುರ್ತು", "ಪೊಲೀಸ್", "ಆಸ್ಪತ್ರೆ", "ಪ್ರವಾಹ", "ರಕ್ಷಣೆ", "ಬೆಂಕಿ"),
        "ml" to listOf("സഹായം", "അപകടം", "അടിയന്തിരം", "പോലീസ്", "ആശുപത്രി", "വെള്ളപ്പൊക്കം", "രക്ഷാപ്രവർത്തനം"),
        "mr" to listOf("मदत", "अपघात", "आणीबाणी", "पोलीस", "रुग्णालय", "पूर", "बचाव", "आग"),
        "gu" to listOf("મદદ", "અકસ્માત", "ઇમરજન્સી", "પોલીસ", "હોસ્પિટલ", "પૂર", "બચાવ", "આગ"),
        "bn" to listOf("সাহায্য", "দুর্ঘটনা", "জরুরি", "পুলিশ", "হাসপাতাল", "বন্যা", "উদ্ধার", "আগুন"),
        "pa" to listOf("ਮਦਦ", "ਹਾਦਸਾ", "ਐਮਰਜੈਂਸੀ", "ਪੁਲਿਸ", "ਹਸਪਤਾਲ", "ਹੜ੍ਹ", "ਬਚਾਅ", "ਅੱਗ")
    )

    fun getEmergencyText(languageCode: String): String {
        return when (languageCode.lowercase()) {
            "te" -> "విజయవాడలో వరద నీరు పెరుగుతోంది. అత్యవసర సహాయం కావలెను."
            "hi" -> "विजयवाड़ा में बाढ़ का पानी बढ़ रहा है। आपातकालीन सहायता की आवश्यकता है।"
            "ta" -> "விஜயவாடாவில் வெள்ள நீர் உயர்கிறது. அவசர உதவி தேவை."
            "kn" -> "ವಿಜಯವಾಡದಲ್ಲಿ ಪ್ರವಾಹ ನೀರು ಏರುತ್ತಿದೆ. ತುರ್ತು ನೆರವು ಬೇಕು."
            "ml" -> "വിജയവാഡയിൽ വെള്ളപ്പൊക്ക ജലനിരപ്പ് ഉയരുന്നു. അടിയന്തിര സഹായം വേണം."
            "mr" -> "विजयवाड्यात पुराचे पाणी वाढत आहे. तातडीची मदत हवी आहे."
            "gu" -> "વિજયવાડામાં પૂરના પાણી વધી રહ્યા છે. કટોકટીની સહાયની જરૂર છે."
            "bn" -> "বিজয়ওয়াড়ায় বন্যার জল বৃদ্ধি পাচ্ছে। জরুরী সহায়তা প্রয়োজন।"
            "pa" -> "ਵਿਜੇਵਾੜਾ ਵਿੱਚ ਹੜ੍ਹ ਦਾ ਪਾਣੀ ਵਧ ਰਿਹਾ ਹੈ। ਐਮਰਜੈਂਸੀ ਮਦਦ ਦੀ ਲੋੜ ਹੈ।"
            else -> "Emergency alert: Flood water levels rising near Vijayawada sector 7."
        }
    }

    /**
     * Get high-priority domain hotwords for the specified language.
     */
    fun getHotwords(languageCode: String): List<String> {
        return emergencyPhrases[languageCode.lowercase()] ?: emergencyPhrases["en"].orEmpty()
    }

    /**
     * Calculate a logit boost factor for token candidate probabilities
     * matching emergency hotword prefixes/characters.
     *
     * @param token Candidate token string
     * @param languageCode Active language pack code
     * @param baseBoost Logit boost factor (e.g., 1.5f - 3.0f)
     * @return Logit modifier score
     */
    fun getBoostForToken(token: String, languageCode: String, baseBoost: Float = 2.0f): Float {
        val hotwords = getHotwords(languageCode)
        val cleanToken = token.trim().lowercase()
        if (cleanToken.isEmpty()) return 0.0f

        for (phrase in hotwords) {
            val cleanPhrase = phrase.lowercase()
            if (cleanPhrase.contains(cleanToken) || cleanToken.contains(cleanPhrase)) {
                return baseBoost
            }
        }
        return 0.0f
    }
}
