package org.odk.collect.android.widgets;

import android.view.View;

import androidx.annotation.Nullable;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.audio.AudioControllerView;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.listeners.WidgetValueChangedListener;
import org.odk.collect.android.support.RobolectricHelpers;
import org.odk.collect.android.support.TestScreenContextActivity;
import org.odk.collect.android.utilities.WidgetAppearanceUtils;
import org.odk.collect.android.widgets.support.FakeQuestionMediaManager;
import org.odk.collect.android.widgets.utilities.AudioDataRequester;
import org.odk.collect.android.widgets.utilities.AudioPlayer;
import org.odk.collect.audioclips.Clip;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.support.RobolectricHelpers.setupMediaPlayerDataSource;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.mockValueChangedListener;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithAnswer;
import static org.odk.collect.android.widgets.support.QuestionWidgetHelpers.promptWithReadOnly;

@RunWith(RobolectricTestRunner.class)
public class AudioWidgetTest {

    private final FakeQuestionMediaManager questionMediaManager = new FakeQuestionMediaManager();
    private final AudioDataRequester audioDataRequester = mock(AudioDataRequester.class);

    private TestScreenContextActivity widgetActivity;
    private FormIndex formIndex;
    private FakeAudioPlayer audioPlayer;

    @Before
    public void setUp() throws Exception {
        widgetActivity = RobolectricHelpers.buildThemedActivity(TestScreenContextActivity.class).get();

        formIndex = mock(FormIndex.class);
        when(formIndex.toString()).thenReturn("questionIndex");

        audioPlayer = new FakeAudioPlayer();
    }

    @Test
    public void usingReadOnlyOption_doesNotShowCaptureAndChooseButtons() {
        AudioWidget widget = createWidget(promptWithReadOnly());
        assertThat(widget.binding.captureButton.getVisibility(), equalTo(GONE));
        assertThat(widget.binding.chooseButton.getVisibility(), equalTo(GONE));
    }

    @Test
    public void getAnswer_whenPromptDoesNotHaveAnswer_returnsNullAndHidesAudioPlayer() {
        AudioWidget widget = createWidget(promptWithAnswer(null));
        assertThat(widget.getAnswer(), nullValue());
        assertThat(widget.binding.audioController.getVisibility(), is(GONE));
    }

    @Test
    public void getAnswer_whenPromptHasAnswer_returnsAnswer() {
        AudioWidget widget = createWidget(promptWithAnswer(new StringData("blah.mp3")));
        assertThat(widget.getAnswer().getDisplayText(), equalTo("blah.mp3"));
    }

    @Test
    public void whenWidgetIsNew_chooseSoundButtonIsNotShown() {
        FormEntryPrompt prompt = promptWithReadOnly();
        when(prompt.getAppearanceHint()).thenReturn(WidgetAppearanceUtils.NEW);
        AudioWidget widget = createWidget(prompt);

        assertThat(widget.binding.chooseButton.getVisibility(), equalTo(GONE));
    }

    @Test
    public void deleteFile_removesWidgetAnswerAndStopsPlayingMedia() {
        AudioWidget widget = createWidget(promptWithAnswer(new StringData("blah.mp3")));
        widget.deleteFile();

        assertThat(widget.getAnswer(), nullValue());
        assertThat(audioPlayer.getCurrentClip(), nullValue());
    }

    @Test
    public void deleteFile_setsFileAsideForDeleting() {
        FormEntryPrompt prompt = promptWithAnswer(new StringData("blah.mp3"));
        when(prompt.getIndex()).thenReturn(formIndex);

        AudioWidget widget = createWidget(prompt);
        widget.deleteFile();

        assertThat(questionMediaManager.originalFiles.get("questionIndex"),
                equalTo(widget.getInstanceFolder() + File.separator + "blah.mp3"));
    }

    @Test
    public void clearAnswer_removesAnswerAndHidesPlayer() {
        AudioWidget widget = createWidget(promptWithAnswer(new StringData("blah.mp3")));
        widget.clearAnswer();

        assertThat(widget.getAnswer(), nullValue());
        assertThat(widget.binding.audioController.getVisibility(), is(GONE));
        assertThat(audioPlayer.getCurrentClip(), nullValue());
    }

    @Test
    public void clearAnswer_setsFileAsideForDeleting() {
        FormEntryPrompt prompt = promptWithAnswer(new StringData("blah.mp3"));
        when(prompt.getIndex()).thenReturn(formIndex);

        AudioWidget widget = createWidget(prompt);
        widget.clearAnswer();

        assertThat(questionMediaManager.originalFiles.get("questionIndex"),
                equalTo(widget.getInstanceFolder() + File.separator + "blah.mp3"));
    }

    @Test
    public void clearAnswer_callsValueChangeListeners() {
        AudioWidget widget = createWidget(promptWithAnswer(new StringData("blah.mp3")));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);
        widget.clearAnswer();

        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void setData_whenFileExists_replacesOriginalFileWithNewFile() throws Exception {
        FormEntryPrompt prompt = promptWithAnswer(new StringData("blah.mp3"));
        when(prompt.getIndex()).thenReturn(formIndex);
        AudioWidget widget = createWidget(prompt);

        File newFile = File.createTempFile("newFile", ".mp3");
        widget.setData(newFile);

        assertThat(questionMediaManager.recentFiles.get("questionIndex"), equalTo(newFile.getAbsolutePath()));
    }

    @Test
    public void setData_whenPromptHasDifferentAudioFile_deletesOriginalAnswer() throws Exception {
        FormEntryPrompt prompt = promptWithAnswer(new StringData("blah.mp3"));
        when(prompt.getIndex()).thenReturn(formIndex);

        AudioWidget widget = createWidget(prompt);

        File newFile = File.createTempFile("newFile", ".mp3");
        widget.setData(newFile);

        assertThat(questionMediaManager.originalFiles.get("questionIndex"),
                equalTo(widget.getInstanceFolder() + File.separator + "blah.mp3"));
    }

    @Test
    public void setData_whenPromptDoesNotHaveAnswer_doesNotDeleteOriginalAnswer() throws Exception {
        AudioWidget widget = createWidget(promptWithAnswer(null));

        File newFile = File.createTempFile("newFile", ".mp3");
        widget.setData(newFile);
        assertThat(questionMediaManager.originalFiles.isEmpty(), equalTo(true));
    }

    @Test
    public void setData_whenPromptHasSameAnswer_doesNotDeleteOriginalAnswer() throws Exception {
        File newFile = File.createTempFile("newFile", ".mp3");
        AudioWidget widget = createWidget(promptWithAnswer(new StringData(newFile.getName())));
        widget.setData(newFile);
        assertThat(questionMediaManager.originalFiles.isEmpty(), equalTo(true));
    }

    @Test
    public void setData_whenFileDoesNotExist_doesNotUpdateWidgetAnswer() {
        AudioWidget widget = createWidget(promptWithAnswer(new StringData("blah.mp3")));
        widget.setData(new File("newFile.mp3"));
        assertThat(widget.getAnswer().getDisplayText(), equalTo("blah.mp3"));
    }

    @Test
    public void setData_whenFileExists_updatesWidgetAnswer() throws Exception {
        AudioWidget widget = createWidget(promptWithAnswer(new StringData("blah.mp3")));

        File newFile = File.createTempFile("newFile", ".mp3");
        widget.setData(newFile);
        assertThat(widget.getAnswer().getDisplayText(), equalTo(newFile.getName()));
    }

    @Test
    public void setData_whenFileExists_updatesPlayerMedia() throws Exception {
        FormEntryPrompt prompt = promptWithAnswer(new StringData("blah.mp3"));
        AudioWidget widget = createWidget(prompt);

        File newFile = File.createTempFile("newFile", ".mp3");
        widget.setData(newFile);

        assertThat(widget.binding.audioController.getVisibility(), is(VISIBLE));
        widget.binding.audioController.binding.play.performClick();

        Clip expectedClip = getExpectedClip(prompt, newFile.getName());
        assertThat(audioPlayer.getCurrentClip(), is(expectedClip));
    }

    @Test
    public void setData_whenFileExists_callsValueChangeListener() throws Exception {
        AudioWidget widget = createWidget(promptWithAnswer(new StringData("blah.mp3")));
        WidgetValueChangedListener valueChangedListener = mockValueChangedListener(widget);

        File newFile = File.createTempFile("newFile", ".mp3");
        widget.setData(newFile);

        verify(valueChangedListener).widgetValueChanged(widget);
    }

    @Test
    public void clickingButtonsForLong_callsOnLongClickListeners() {
        View.OnLongClickListener listener = mock(View.OnLongClickListener.class);
        AudioWidget widget = createWidget(promptWithAnswer(null));
        widget.setOnLongClickListener(listener);

        widget.binding.captureButton.performLongClick();
        widget.binding.chooseButton.performLongClick();

        verify(listener).onLongClick(widget.binding.captureButton);
        verify(listener).onLongClick(widget.binding.chooseButton);
    }

    @Test
    public void clickingChooseButton_requestsAudioFile() {
        FormEntryPrompt prompt = promptWithAnswer(null);
        AudioWidget widget = createWidget(prompt);

        widget.binding.chooseButton.performClick();
        verify(audioDataRequester).requestFile(prompt);
    }

    @Test
    public void clickingCaptureButton_requestsRecording() {
        FormEntryPrompt prompt = promptWithAnswer(null);
        AudioWidget widget = createWidget(prompt);

        widget.binding.captureButton.performClick();
        verify(audioDataRequester).requestRecording(prompt);
    }

    @Test
    public void afterSetBinaryData_clickingPlayAndPause_playsAndPausesAudio() throws Exception {
        FormEntryPrompt prompt = promptWithAnswer(null);
        AudioWidget widget = createWidget(prompt);

        File audioFile = File.createTempFile("blah", ".mp3");
        Clip expectedClip = getExpectedClip(prompt, audioFile.getName());
        widget.setData(audioFile);

        AudioControllerView audioController = widget.binding.audioController;
        assertThat(audioController.getVisibility(), is(VISIBLE));
        audioController.binding.play.performClick();
        assertThat(audioPlayer.getCurrentClip(), is(expectedClip));

        audioController.binding.play.performClick();
        assertThat(audioPlayer.getCurrentClip(), is(expectedClip));
        assertThat(audioPlayer.isPaused(), is(true));

        audioController.binding.play.performClick();
        assertThat(audioPlayer.getCurrentClip(), is(expectedClip));
        assertThat(audioPlayer.isPaused(), is(false));
    }

    @Test
    public void afterSetBinaryData_canSkipClipForward() throws Exception {
        FormEntryPrompt prompt = promptWithAnswer(null);

        File audioFile = File.createTempFile("blah", ".mp3");
        Clip expectedClip = getExpectedClip(prompt, audioFile.getName());
        setupMediaPlayerDataSource(expectedClip.getURI(), 322450);

        AudioWidget widget = createWidget(prompt);
        widget.setData(audioFile);

        AudioControllerView audioController = widget.binding.audioController;
        audioController.binding.fastForwardBtn.performClick();
        assertThat(audioPlayer.getPosition(expectedClip.getClipID()), is(5000));
    }

    @Test
    public void afterSetBinaryData_whenPositionOfClipChanges_updatesPosition() throws Exception {
        FormEntryPrompt prompt = promptWithAnswer(null);

        File audioFile = File.createTempFile("blah", ".mp3");
        Clip expectedClip = getExpectedClip(prompt, audioFile.getName());
        setupMediaPlayerDataSource(expectedClip.getURI(), 322450);

        AudioWidget widget = createWidget(prompt);
        widget.setData(audioFile);

        AudioControllerView audioController = widget.binding.audioController;
        assertThat(audioController.binding.currentDuration.getText().toString(), is("00:00"));

        audioPlayer.setPosition(expectedClip.getClipID(), 42000);
        assertThat(audioController.binding.currentDuration.getText().toString(), is("00:42"));
    }

    @Test
    public void afterSetBinaryData_showsDurationOfAudio() throws Exception {
        FormEntryPrompt prompt = promptWithAnswer(null);

        File audioFile = File.createTempFile("blah", ".mp3");
        Clip expectedClip = getExpectedClip(prompt, audioFile.getName());
        setupMediaPlayerDataSource(expectedClip.getURI(), 322450);

        AudioWidget widget = createWidget(prompt);
        widget.setData(audioFile);

        AudioControllerView audioController = widget.binding.audioController;
        assertThat(audioController.binding.totalDuration.getText().toString(), is("05:22"));
    }

    public AudioWidget createWidget(FormEntryPrompt prompt) {
        return new AudioWidget(
                widgetActivity,
                new QuestionDetails(prompt, "formAnalyticsID"),
                questionMediaManager,
                audioPlayer,
                audioDataRequester
        );
    }

    @NotNull
    private Clip getExpectedClip(FormEntryPrompt prompt, String fileName) {
        return new Clip(
                "audio:" + prompt.getIndex().toString(),
                new File("null", fileName).getAbsolutePath() // This is instanceFolder/fileName
        );
    }

    private static class FakeAudioPlayer implements AudioPlayer {

        private final Map<String, Consumer<Boolean>> playingChangedListeners = new HashMap<>();
        private final Map<String, Consumer<Integer>> positionChangedListeners = new HashMap<>();
        private final Map<String, Integer> positions = new HashMap<>();

        private boolean paused;
        private Clip clip;

        @Override
        public void play(Clip clip) {
            this.clip = clip;
            paused = false;
            playingChangedListeners.get(clip.getClipID()).accept(true);
        }

        @Override
        public void pause() {
            paused = true;
            playingChangedListeners.get(clip.getClipID()).accept(false);
        }

        @Override
        public void setPosition(String clipId, Integer position) {
            positions.put(clipId, position);
            positionChangedListeners.get(clipId).accept(position);
        }

        @Override
        public void onPlayingChanged(String clipID, Consumer<Boolean> playingConsumer) {
            playingChangedListeners.put(clipID, playingConsumer);
        }

        @Override
        public void onPositionChanged(String clipID, Consumer<Integer> positionConsumer) {
            positionChangedListeners.put(clipID, positionConsumer);
        }

        @Override
        public void stop() {
            clip = null;
        }

        @Nullable
        public Clip getCurrentClip() {
            return clip;
        }

        public boolean isPaused() {
            return paused;
        }

        public Integer getPosition(String clipId) {
            return positions.get(clipId);
        }
    }
}