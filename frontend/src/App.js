import React, { useState, useEffect, useRef } from 'react';
import './App.css';

function App() {
  const [language, setLanguage] = useState('en');
  const [spn, setSpn] = useState('');
  const [fmi, setFmi] = useState('');
  const [analysis, setAnalysis] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [isListening, setIsListening] = useState(false);
  const [showWelcome, setShowWelcome] = useState(true);
  const [followUpAnswer, setFollowUpAnswer] = useState('');
  const [followUpLoading, setFollowUpLoading] = useState(false);
  const [chatQuestion, setChatQuestion] = useState('');
  const [chatAnswer, setChatAnswer] = useState('');
  const [chatLoading, setChatLoading] = useState(false);
  const [isChatListening, setIsChatListening] = useState(false);
  const recognitionRef = useRef(null);
  const chatRecognitionRef = useRef(null);

  const translations = {
    en: {
      title: 'Diesel Truck Fault Code Advisor',
      subtitle: 'Get instant AI-powered diagnostics for your truck',
      welcomeTitle: 'Welcome!',
      welcomeMessage: 'Click the button below to start. I\'ll help you understand your truck\'s fault codes in simple terms.',
      startButton: 'Click to Start',
      spnLabel: 'SPN Number',
      fmiLabel: 'FMI Number',
      spnPlaceholder: 'Enter SPN (e.g., 157)',
      fmiPlaceholder: 'Enter FMI (e.g., 3)',
      analyzeButton: 'Analyze Fault Code',
      voiceButton: 'Speak Fault Code',
      listening: 'Listening...',
      explanation: 'What This Means',
      rootCauses: 'Common Root Causes',
      fixInstructions: 'How to Fix It',
      toolsNeeded: 'Tools You\'ll Need',
      partsToBuy: 'Parts to Buy',
      loading: 'Analyzing your fault code...',
      error: 'Error',
      switchLanguage: 'Español',
      greeting: 'Hi! Tell me your fault code or type it in, whatever is easier for you.',
      followUpTitle: 'Have More Questions?',
      followUpButtons: {
        simpler: 'Explain this in simpler words',
        location: 'Where exactly is this part?',
        buy: 'Where can I buy this part?',
        safe: 'Is it safe to drive?'
      },
      chatTitle: 'Ask the Truck Assistant',
      chatPlaceholder: 'Ask any question about your Detroit Series 60...',
      chatButton: 'Ask',
      chatVoiceButton: 'Speak Question',
      chatLoading: 'Thinking...'
    },
    es: {
      title: 'Asesor de Códigos de Falla para Camiones Diesel',
      subtitle: 'Obtén diagnósticos instantáneos con inteligencia artificial',
      welcomeTitle: '¡Bienvenido!',
      welcomeMessage: 'Haz clic en el botón para comenzar. Te ayudaré a entender los códigos de falla de tu camión en términos simples.',
      startButton: 'Haz Clic para Comenzar',
      spnLabel: 'Número SPN',
      fmiLabel: 'Número FMI',
      spnPlaceholder: 'Ingresa SPN (ej., 157)',
      fmiPlaceholder: 'Ingresa FMI (ej., 3)',
      analyzeButton: 'Analizar Código de Falla',
      voiceButton: 'Decir Código de Falla',
      listening: 'Escuchando...',
      explanation: 'Qué Significa Esto',
      rootCauses: 'Causas Comunes',
      fixInstructions: 'Cómo Repararlo',
      toolsNeeded: 'Herramientas Necesarias',
      partsToBuy: 'Partes a Comprar',
      loading: 'Analizando tu código de falla...',
      error: 'Error',
      switchLanguage: 'English',
      greeting: '¡Hola! Dime tu código de falla o escríbelo, lo que sea más fácil para ti.',
      followUpTitle: '¿Tienes Más Preguntas?',
      followUpButtons: {
        simpler: 'Explica esto más simple',
        location: '¿Dónde está exactamente esta parte?',
        buy: '¿Dónde puedo comprar esta parte?',
        safe: '¿Es seguro manejar?'
      },
      chatTitle: 'Pregunta al Asistente del Camión',
      chatPlaceholder: 'Pregunta cualquier cosa sobre tu Detroit Series 60...',
      chatButton: 'Preguntar',
      chatVoiceButton: 'Hablar Pregunta',
      chatLoading: 'Pensando...'
    }
  };

  const t = translations[language];

  // Play bilingual greeting after user clicks start
  const playGreeting = () => {
    if ('speechSynthesis' in window) {
      const greetingText = 
        "Hi! Tell me your fault code or type it in, whatever is easier for you. " +
        "¡Hola! Dime tu código de falla o escríbelo, lo que sea más fácil para ti.";
      
      const utterance = new SpeechSynthesisUtterance(greetingText);
      utterance.rate = 0.9;
      utterance.pitch = 1;
      utterance.volume = 1;
      
      window.speechSynthesis.speak(utterance);
    }
  };

  const handleStart = () => {
    setShowWelcome(false);
    playGreeting();
  };

  // Text-to-speech function with toggle stop/start
  const speakText = (text, lang) => {
    if ('speechSynthesis' in window) {
      if (window.speechSynthesis.speaking) {
        window.speechSynthesis.cancel();
        return;
      }
      
      const cleanedText = cleanResponseText(text);
      const utterance = new SpeechSynthesisUtterance(cleanedText);
      utterance.rate = 0.9;
      utterance.pitch = 1;
      utterance.volume = 1;
      utterance.lang = lang === 'es' ? 'es-ES' : 'en-US';

      const voices = window.speechSynthesis.getVoices();
      const preferredVoice = voices.find(v => 
        lang === 'es' 
          ? v.name.includes('Mónica') || v.name.includes('Paulina') || v.name.includes('Spanish')
          : v.name.includes('Samantha') || v.name.includes('Google US') || v.name.includes('Alex')
      );
      if (preferredVoice) utterance.voice = preferredVoice;

      
      window.speechSynthesis.speak(utterance);
    }
  };

  // Clean response text by removing trailing numbers and artifacts
  const cleanResponseText = (text) => {
    if (!text) return '';
    
    // Remove trailing numbers at end of sentences (e.g., "sentence.3" or "sentence. 5")
    let cleaned = text.replace(/\.\s*\d+\s*$/g, '.');
    cleaned = cleaned.replace(/\.\s*\d+\s*\./g, '.');
    
    // Remove standalone numbers at the end
    cleaned = cleaned.replace(/\s+\d+\s*$/g, '');
    
    // Remove multiple spaces
    cleaned = cleaned.replace(/\s+/g, ' ');
    
    return cleaned.trim();
  };

  // Cancel speech when component unmounts or when new content loads
  useEffect(() => {
    return () => {
      if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel();
      }
    };
  }, []);

  // Cancel speech when analysis changes
  useEffect(() => {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }
  }, [analysis, followUpAnswer, chatAnswer]);

  // Initialize speech recognition for fault codes
  useEffect(() => {
    if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
      const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
      recognitionRef.current = new SpeechRecognition();
      recognitionRef.current.continuous = false;
      recognitionRef.current.interimResults = false;
      recognitionRef.current.lang = language === 'en' ? 'en-US' : 'es-ES';

      recognitionRef.current.onresult = (event) => {
        const transcript = event.results[0][0].transcript.toLowerCase();
        console.log('Voice input:', transcript);
        
        // Extract numbers from speech
        const numbers = transcript.match(/\d+/g);
        if (numbers && numbers.length >= 2) {
          setSpn(numbers[0]);
          setFmi(numbers[1]);
        } else if (numbers && numbers.length === 1) {
          setSpn(numbers[0]);
        }
        
        setIsListening(false);
      };

      recognitionRef.current.onerror = (event) => {
        console.error('Speech recognition error:', event.error);
        setIsListening(false);
      };

      recognitionRef.current.onend = () => {
        setIsListening(false);
      };
    }
  }, [language]);

  // Initialize speech recognition for chat
  useEffect(() => {
    if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
      const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
      chatRecognitionRef.current = new SpeechRecognition();
      chatRecognitionRef.current.continuous = false;
      chatRecognitionRef.current.interimResults = false;
      chatRecognitionRef.current.lang = language === 'en' ? 'en-US' : 'es-ES';

      chatRecognitionRef.current.onresult = (event) => {
        const transcript = event.results[0][0].transcript;
        console.log('Chat voice input:', transcript);
        setChatQuestion(transcript);
        setIsChatListening(false);
      };

      chatRecognitionRef.current.onerror = (event) => {
        console.error('Chat speech recognition error:', event.error);
        setIsChatListening(false);
      };

      chatRecognitionRef.current.onend = () => {
        setIsChatListening(false);
      };
    }
  }, [language]);

  const startVoiceInput = () => {
    if (recognitionRef.current) {
      setIsListening(true);
      recognitionRef.current.start();
    } else {
      alert('Voice input is not supported in your browser. Please use Chrome or Edge.');
    }
  };

  const startChatVoiceInput = () => {
    if (chatRecognitionRef.current) {
      setIsChatListening(true);
      chatRecognitionRef.current.start();
    } else {
      alert('Voice input is not supported in your browser. Please use Chrome or Edge.');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setAnalysis(null);
    setFollowUpAnswer('');

    try {
      const response = await fetch('http://localhost:8080/api/analyze', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          spn: spn,
          fmi: fmi,
          language: language
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      setAnalysis(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleFollowUpQuestion = async (questionType) => {
    setFollowUpLoading(true);
    setFollowUpAnswer('');

    const questions = {
      en: {
        simpler: `Please explain the fault code SPN ${spn} FMI ${fmi} in even simpler terms, as if explaining to someone with no mechanical knowledge at all. Use everyday examples and analogies.`,
        location: `Where exactly is the part related to fault code SPN ${spn} FMI ${fmi} located on a diesel truck? Describe the physical location in simple terms with landmarks (like "near the engine", "by the exhaust pipe", etc.)`,
        buy: `Where can I buy the parts needed to fix fault code SPN ${spn} FMI ${fmi}? Please suggest specific stores or online retailers, and mention if I should look for OEM or aftermarket parts.`,
        safe: `Is it safe to continue driving my truck with fault code SPN ${spn} FMI ${fmi}? What are the risks? Should I stop immediately or can I drive to a repair shop?`
      },
      es: {
        simpler: `Por favor explica el código de falla SPN ${spn} FMI ${fmi} en términos aún más simples, como si le explicaras a alguien sin ningún conocimiento mecánico. Usa ejemplos cotidianos y analogías.`,
        location: `¿Dónde está exactamente ubicada la parte relacionada con el código de falla SPN ${spn} FMI ${fmi} en un camión diesel? Describe la ubicación física en términos simples con puntos de referencia (como "cerca del motor", "junto al tubo de escape", etc.)`,
        buy: `¿Dónde puedo comprar las partes necesarias para arreglar el código de falla SPN ${spn} FMI ${fmi}? Por favor sugiere tiendas específicas o minoristas en línea, y menciona si debo buscar partes OEM o del mercado secundario.`,
        safe: `¿Es seguro seguir manejando mi camión con el código de falla SPN ${spn} FMI ${fmi}? ¿Cuáles son los riesgos? ¿Debo detenerme inmediatamente o puedo manejar hasta un taller?`
      }
    };

    const question = questions[language][questionType];

    try {
      const response = await fetch('http://localhost:8080/api/followup', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          spn: spn,
          fmi: fmi,
          question: question,
          language: language
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      // Extract the answer from the response
      setFollowUpAnswer(data.answer || 'No answer available');
    } catch (err) {
      setFollowUpAnswer(`Error: ${err.message}`);
    } finally {
      setFollowUpLoading(false);
    }
  };

  const handleChatSubmit = async (e) => {
    e.preventDefault();
    if (!chatQuestion.trim()) return;

    setChatLoading(true);
    setChatAnswer('');

    try {
      const response = await fetch('http://localhost:8080/api/chat', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          question: chatQuestion,
          language: language
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      setChatAnswer(data.answer || 'No answer available');
    } catch (err) {
      setChatAnswer(`Error: ${err.message}`);
    } finally {
      setChatLoading(false);
    }
  };

  const toggleLanguage = () => {
    setLanguage(language === 'en' ? 'es' : 'en');
  };

  if (showWelcome) {
    return (
      <div className="App welcome-screen">
        <div className="welcome-content">
          <h1 className="welcome-title">{t.welcomeTitle}</h1>
          <p className="welcome-message">{t.welcomeMessage}</p>
          <button className="start-button" onClick={handleStart}>
            🚛 {t.startButton}
          </button>
          <button className="language-toggle-welcome" onClick={toggleLanguage}>
            🌐 {t.switchLanguage}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="App">
      <header className="App-header">
        <div className="header-content">
          <h1>{t.title}</h1>
          <p className="subtitle">{t.subtitle}</p>
          <button className="language-toggle" onClick={toggleLanguage}>
            🌐 {t.switchLanguage}
          </button>
        </div>
      </header>

      <main className="App-main">
        <div className="form-container">
          <form onSubmit={handleSubmit}>
            <div className="input-group">
              <label htmlFor="spn">{t.spnLabel}</label>
              <input
                type="text"
                id="spn"
                value={spn}
                onChange={(e) => setSpn(e.target.value)}
                placeholder={t.spnPlaceholder}
                required
              />
            </div>

            <div className="input-group">
              <label htmlFor="fmi">{t.fmiLabel}</label>
              <input
                type="text"
                id="fmi"
                value={fmi}
                onChange={(e) => setFmi(e.target.value)}
                placeholder={t.fmiPlaceholder}
                required
              />
            </div>

            <div className="button-group">
              <button type="submit" className="submit-button" disabled={loading}>
                {loading ? t.loading : t.analyzeButton}
              </button>
              
              <button
                type="button"
                className={`voice-button ${isListening ? 'listening' : ''}`}
                onClick={startVoiceInput}
                disabled={loading || isListening}
              >
                🎤 {isListening ? t.listening : t.voiceButton}
              </button>
            </div>
          </form>
        </div>

        {error && (
          <div className="error-message">
            <h3>{t.error}</h3>
            <p>{error}</p>
          </div>
        )}

        {analysis && (
          <>
            <div className="results-container">
              <div className="result-section">
                <div className="section-header">
                  <h2>📋 {t.explanation}</h2>
                  <button
                    className="speaker-button"
                    onClick={() => speakText(t.explanation + '. ' + analysis.explanation, language)}
                    title="Listen to explanation"
                  >
                    🔊
                  </button>
                </div>
                <p className="explanation-text">{cleanResponseText(analysis.explanation)}</p>
              </div>

              <div className="result-section">
                <div className="section-header">
                  <h2>🔍 {t.rootCauses}</h2>
                  <button
                    className="speaker-button"
                    onClick={() => speakText(t.explanation + '. ' + analysis.explanation, language)}
                    title="Listen to root causes"
                  >
                    🔊
                  </button>
                </div>
                <ul className="causes-list">
                  {analysis.rootCauses.map((cause, index) => (
                    <li key={index}>{cleanResponseText(cause)}</li>
                  ))}
                </ul>
              </div>

              <div className="result-section">
                <div className="section-header">
                  <h2>🔧 {t.fixInstructions}</h2>
                  <button
                    className="speaker-button"
                    onClick={() => speakText(t.explanation + '. ' + analysis.explanation, language)}
                    title="Listen to fix instructions"
                  >
                    🔊
                  </button>
                </div>
                <ol className="instructions-list">
                  {analysis.fixInstructions.map((instruction, index) => (
                    <li key={index}>{cleanResponseText(instruction)}</li>
                  ))}
                </ol>
              </div>

              <div className="result-section">
                <div className="section-header">
                  <h2>🛠️ {t.toolsNeeded}</h2>
                  <button
                    className="speaker-button"
                    onClick={() => speakText(t.explanation + '. ' + analysis.explanation, language)}
                    title="Listen to tools needed"
                  >
                    🔊
                  </button>
                </div>
                <ul className="tools-list">
                  {analysis.toolsNeeded.map((tool, index) => (
                    <li key={index}>{cleanResponseText(tool)}</li>
                  ))}
                </ul>
              </div>

              <div className="result-section">
                <div className="section-header">
                  <h2>🛒 {t.partsToBuy}</h2>
                  <button
                    className="speaker-button"
                    onClick={() => speakText(t.explanation + '. ' + analysis.explanation, language)}
                    title="Listen to parts to buy"
                  >
                    🔊
                  </button>
                </div>
                <ul className="parts-list">
                  {analysis.partsToBuy.map((part, index) => (
                    <li key={index}>{cleanResponseText(part)}</li>
                  ))}
                </ul>
              </div>
            </div>

            <div className="follow-up-section">
              <h2>💬 {t.followUpTitle}</h2>
              <div className="follow-up-buttons">
                <button
                  className="follow-up-button"
                  onClick={() => handleFollowUpQuestion('simpler')}
                  disabled={followUpLoading}
                >
                  💡 {t.followUpButtons.simpler}
                </button>
                <button
                  className="follow-up-button"
                  onClick={() => handleFollowUpQuestion('location')}
                  disabled={followUpLoading}
                >
                  📍 {t.followUpButtons.location}
                </button>
                <button
                  className="follow-up-button"
                  onClick={() => handleFollowUpQuestion('buy')}
                  disabled={followUpLoading}
                >
                  🛒 {t.followUpButtons.buy}
                </button>
                <button
                  className="follow-up-button"
                  onClick={() => handleFollowUpQuestion('safe')}
                  disabled={followUpLoading}
                >
                  ⚠️ {t.followUpButtons.safe}
                </button>
              </div>

              {followUpLoading && (
                <div className="follow-up-loading">
                  <p>🤔 Thinking...</p>
                </div>
              )}

              {followUpAnswer && (
                <div className="follow-up-answer">
                  <div className="section-header">
                    <h3>Answer:</h3>
                    <button
                      className="speaker-button"
                      onClick={() => speakText(followUpAnswer, language)}
                      title="Listen to answer"
                    >
                      🔊
                    </button>
                  </div>
                  <p>{cleanResponseText(followUpAnswer)}</p>
                </div>
              )}
            </div>
          </>
        )}

        {/* Chat Assistant Section */}
        <div className="chat-section">
          <h2>🤖 {t.chatTitle}</h2>
          <form onSubmit={handleChatSubmit} className="chat-form">
            <div className="chat-input-group">
              <input
                type="text"
                value={chatQuestion}
                onChange={(e) => setChatQuestion(e.target.value)}
                placeholder={t.chatPlaceholder}
                className="chat-input"
                disabled={chatLoading}
              />
              <button
                type="button"
                className={`chat-voice-button ${isChatListening ? 'listening' : ''}`}
                onClick={startChatVoiceInput}
                disabled={chatLoading || isChatListening}
              >
                🎤
              </button>
              <button
                type="submit"
                className="chat-submit-button"
                disabled={chatLoading || !chatQuestion.trim()}
              >
                {chatLoading ? t.chatLoading : t.chatButton}
              </button>
            </div>
          </form>

          {chatAnswer && (
            <div className="chat-answer">
              <div className="section-header">
                <h3>Answer:</h3>
                <button
                  className="speaker-button"
                  onClick={() => speakText(chatAnswer, language)}
                  title="Listen to answer"
                >
                  🔊
                </button>
              </div>
              <p>{cleanResponseText(chatAnswer)}</p>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}

export default App;

// Made with Bob
