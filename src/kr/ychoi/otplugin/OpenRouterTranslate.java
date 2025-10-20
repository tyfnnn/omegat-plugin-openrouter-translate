package kr.ychoi.otplugin;

import java.awt.GridBagConstraints;
import java.awt.Window;
import java.util.*;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.omegat.core.machinetranslators.BaseCachedTranslate;
import org.omegat.gui.exttrans.MTConfigDialog;
import org.omegat.util.Language;
import org.omegat.util.Preferences;
import org.omegat.util.WikiGet;
import org.json.*;
import org.omegat.gui.glossary.GlossaryEntry;
import org.omegat.gui.glossary.GlossarySearcher;
import org.omegat.core.Core;
import org.omegat.core.data.SourceTextEntry;

/*
 * OpenRouter Translate plugin for OmegaT
 * Modified from OpenAI Translate plugin to use OpenRouter API
 * based on Naver Translate plugin by ParanScreen https://github.com/ParanScreen/omegat-plugin-navertranslate
 * licensed under GNU GPLv2 and modified by ychoi
 */


public class OpenRouterTranslate extends BaseCachedTranslate {

    // GEÄNDERT: OpenRouter API URL statt OpenAI
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";

    private static final String BASE_PROMPT =
            "You are a translation tool integrated in a CAT (Computer-Assisted Translation) tool. " +
                    "Translate the following text from %s to %s. Preserve the tags in the text and keep any segmentations intact.\n\n";

    // GEÄNDERT: Parameter-Namen für OpenRouter
    private static final String PARAM_API_KEY = "openrouter.api.key";
    private static final String PARAM_MODEL = "openrouter.model";
    private static final String PARAM_TEMPERATURE = "openrouter.temperature";
    private static final String PARAM_CUSTOM_PROMPT = "custom.prompt";

    // GEÄNDERT: Standard-Modell für OpenRouter (z.B. anthropic/claude-3.5-sonnet)
    private static final String DEFAULT_MODEL = "anthropic/claude-3.5-sonnet";
    private static final String DEFAULT_TEMPERATURE = "0";
    private static final String DEFAULT_CUSTOM_PROMPT = "";

    private JTextField apiKeyField;
    private JTextField modelField;
    private JTextField tempField;
    private JTextArea promptField;

    @Override
    protected String getPreferenceName() {
        return "allow_openrouter_translate";
    }

    public String getName() {
        if (Preferences.getPreferenceDefault(PARAM_API_KEY, "").isEmpty()) {
            return "OpenRouter Translate (API Key Required)";
        } else {
            return "OpenRouter Translate";
        }
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public void showConfigurationUI(Window parent) {
        JPanel configPanel = new JPanel(new java.awt.GridBagLayout());
        GridBagConstraints gridBagConstraints;

        int uiRow = 0;

        // API Key
        JLabel apiKeyLabel = new JLabel("OpenRouter API Key:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 5);
        configPanel.add(apiKeyLabel, gridBagConstraints);

        apiKeyField = new JTextField(Preferences.getPreferenceDefault(PARAM_API_KEY, ""), 52);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        configPanel.add(apiKeyField, gridBagConstraints);

        uiRow++;

        // Model
        JLabel modelLabel = new JLabel("Model:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 5);
        configPanel.add(modelLabel, gridBagConstraints);

        modelField = new JTextField(Preferences.getPreferenceDefault(PARAM_MODEL, DEFAULT_MODEL), 52);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        configPanel.add(modelField, gridBagConstraints);

        uiRow++;

        // Temperature
        JLabel tempLabel = new JLabel("Temperature:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 5);
        configPanel.add(tempLabel, gridBagConstraints);

        tempField = new JTextField(Preferences.getPreferenceDefault(PARAM_TEMPERATURE, DEFAULT_TEMPERATURE), 52);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        configPanel.add(tempField, gridBagConstraints);

        uiRow++;

        // Custom Prompt
        JLabel promptLabel = new JLabel("Custom Prompt:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 5);
        configPanel.add(promptLabel, gridBagConstraints);

        promptField = new JTextArea(Preferences.getPreferenceDefault(PARAM_CUSTOM_PROMPT, DEFAULT_CUSTOM_PROMPT), 5, 52);
        promptField.setLineWrap(true);
        promptField.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(promptField);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        configPanel.add(scrollPane, gridBagConstraints);

        MTConfigDialog dialog = new MTConfigDialog(parent, getName()) {
            @Override
            protected void onConfirm() {
                String key = apiKeyField.getText().trim();
                Preferences.setPreference(PARAM_API_KEY, key);

                String model = modelField.getText().trim();
                if (!model.isEmpty()) {
                    Preferences.setPreference(PARAM_MODEL, model);
                }

                String temp = tempField.getText().trim();
                if (!temp.isEmpty()) {
                    Preferences.setPreference(PARAM_TEMPERATURE, temp);
                }

                String customPrompt = promptField.getText().trim();
                Preferences.setPreference(PARAM_CUSTOM_PROMPT, customPrompt);
            }
        };

        dialog.show(configPanel);
    }

    protected String translate(Language sLang, Language tLang, String text) throws Exception {
        String apiKey = Preferences.getPreferenceDefault(PARAM_API_KEY, "");
        String model = Preferences.getPreferenceDefault(PARAM_MODEL, DEFAULT_MODEL);
        float temperature = Float.parseFloat(Preferences.getPreferenceDefault(PARAM_TEMPERATURE, DEFAULT_TEMPERATURE));

        // Projekt SourceTextEntry finden
        List<SourceTextEntry> entries = Core.getProject().getAllEntries();
        SourceTextEntry matchingEntry = null;

        for (SourceTextEntry entry : entries) {
            if (entry.getSrcText().equals(text)) {
                matchingEntry = entry;
                break;
            }
        }

        List<GlossaryEntry> glossaryEntries = new ArrayList<>();
        if (matchingEntry != null) {
            // GlossarySearcher verwenden
            GlossarySearcher glossarySearcher = new GlossarySearcher(Core.getProject().getSourceTokenizer(), sLang, true);
            glossaryEntries = glossarySearcher.searchSourceMatches(matchingEntry, Core.getGlossaryManager().getGlossaryEntries(text));
        }

        // System- und User-Prompt erstellen
        String systemPrompt = createSystemPrompt(sLang, tLang, glossaryEntries);
        System.out.println("System Prompt: " + systemPrompt);
        String userPrompt = text;
        System.out.println("User Prompt: " + userPrompt);

        // OpenRouter API Anfrage
        return requestTranslation(systemPrompt, userPrompt, apiKey, model, temperature);
    }


    private String createSystemPrompt(Language sLang, Language tLang, List<GlossaryEntry> glossaryEntries) {
        String customPrompt = Preferences.getPreferenceDefault(PARAM_CUSTOM_PROMPT, DEFAULT_CUSTOM_PROMPT);

        StringBuilder promptBuilder = new StringBuilder();

        // Basis-Instruktionen
        promptBuilder.append(String.format(BASE_PROMPT, sLang.getLanguage(), tLang.getLanguage()));

        // Glossar hinzufügen, falls vorhanden
        if (!glossaryEntries.isEmpty()) {
            promptBuilder.append("Glossary:\n");
            for (GlossaryEntry entry : glossaryEntries) {
                String[] locTerms = entry.getLocTerms(false);
                String locTerm = locTerms.length > 0 ? locTerms[0] : "";
                promptBuilder.append(entry.getSrcText()).append("\t").append(locTerm).append("\n");
            }
        }

        // Benutzerdefinierter Prompt
        if (!customPrompt.isEmpty()) {
            promptBuilder.append("\n").append(customPrompt).append("\n");
        }

        return promptBuilder.toString();
    }

    private String requestTranslation(String systemPrompt, String userPrompt, String apiKey, String model, float temperature) throws Exception {
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
        messages.put(new JSONObject().put("role", "user").put("content", userPrompt));

        Map<String, String> headers = new TreeMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + apiKey);
        // OPTIONAL: OpenRouter-spezifische Header
        // headers.put("HTTP-Referer", "https://your-site.com"); // Optional: Ihre Website
        // headers.put("X-Title", "OmegaT Translation Plugin"); // Optional: App-Name

        JSONObject requestBody = new JSONObject()
                .put("model", model)
                .put("messages", messages)
                .put("temperature", temperature);

        String body = requestBody.toString();

        try {
            String response = WikiGet.postJSON(API_URL, body, headers);
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices.length() > 0) {
                JSONObject choice = choices.getJSONObject(0);
                if (choice.has("message")) {
                    JSONObject message = choice.getJSONObject("message");
                    return message.getString("content").trim();
                }
            }
            return "Translation failed";
        } catch (Exception e) {
            return "Error contacting OpenRouter API: " + e.getMessage();
        }
    }
}