package ap.andruav_ap.activities.phone;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import ap.andruav_ap.R;
import ap.andruav_ap.activities.ThemeColors;
import ap.andruav_ap.communication.telemetry.AndruavSMSClientParser;
import ap.andruav_ap.communication.telemetry.SmsGateVerdict;
import ap.andruav_ap.communication.telemetry.SmsKeywords;
import ap.andruavmiddlelibrary.preference.Preference;

/***
 * AN-0139 (TASK-118): the DRY RUN sheet of the SMS tab. The user picks a
 * sender (an allowed contact or "unknown") and a message (a keyword, rtl,
 * a wrong PIN, or edited text); the sheet runs the pure
 * {@link AndruavSMSClientParser#evaluate} against the saved preferences and
 * lists each gate with its detail, then the action that would follow.
 * Nothing here sends an SMS, notifies the GCS, counts a PIN failure or runs
 * a command.
 *
 * LIVE TEST is the escape hatch: a warning dialog first, and only RUN LIVE
 * calls executeCommand(sender, message, testRun=true), which does what a
 * real SMS does with every GCS notice prefixed "TEST: ".
 */
public class SmsDryRunSheet extends BottomSheetDialogFragment {

    public static final String TAG = "SmsDryRunSheet";

    /** Sender used for the "unknown" pick - never on the allow-list. */
    private static final String UNKNOWN_SENDER = "+10000000000";
    /** Message-pick sentinel for the wrong-PIN case. */
    private static final String TOKEN_WRONG_PIN = "\u0001wrong_pin";

    private final List<String> mSenderLabels = new ArrayList<>();
    private final List<String> mSenderNumbers = new ArrayList<>();
    private final List<String> mMessageTokens = new ArrayList<>();
    private final List<String> mMessageLabels = new ArrayList<>();

    private int mSenderIndex = 0;
    private int mMessageIndex = 0;
    private String mMessage = "";
    private boolean mEditing;
    /** The last computed verdict - the LIVE TEST dialog reads it. */
    private SmsGateVerdict mVerdict;

    private LinearLayout mFromGroup;
    private LinearLayout mMessageGroup;
    private TextView mMsgPreview;
    private EditText mMsgEdit;
    private TextView mBtnEdit;
    private LinearLayout mGatesList;
    private TextView mTxtResult;

    public static SmsDryRunSheet newInstance() {
        return new SmsDryRunSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_sms_dryrun, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mFromGroup = view.findViewById(R.id.phone_dry_from);
        mMessageGroup = view.findViewById(R.id.phone_dry_message);
        mMsgPreview = view.findViewById(R.id.phone_dry_msg_preview);
        mMsgEdit = view.findViewById(R.id.phone_dry_msg_edit);
        mBtnEdit = view.findViewById(R.id.phone_dry_btn_edit);
        mGatesList = view.findViewById(R.id.phone_dry_gates);
        mTxtResult = view.findViewById(R.id.phone_dry_result);

        view.findViewById(R.id.phone_dry_btn_check)
                .setOnClickListener(v -> evaluateAndRender());
        view.findViewById(R.id.phone_dry_btn_live)
                .setOnClickListener(v -> showLiveWarning());

        mBtnEdit.setOnClickListener(v -> toggleEdit());
        mMsgEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                mMessage = s.toString();
                evaluateAndRender();
            }
        });

        buildPickers();
        selectMessage(mMessageIndex);
    }

    ////////// pickers

    private void buildPickers() {
        mSenderLabels.clear();
        mSenderNumbers.clear();
        for (final String entry : Preference.getSmsAllowedContacts(null)) {
            final String name = SmsKeywords.entryName(entry);
            mSenderLabels.add(name.isEmpty()
                    ? PhoneSmsFragment.maskNumber(SmsKeywords.entryNumber(entry)) : name);
            mSenderNumbers.add(SmsKeywords.entryNumber(entry));
        }
        mSenderLabels.add(getString(R.string.phone_sms_dry_unknown));
        mSenderNumbers.add(UNKNOWN_SENDER);

        mMessageTokens.clear();
        mMessageLabels.clear();
        for (final String kw : SmsKeywords.parse(Preference.getSmsKeywords(null))) {
            mMessageTokens.add(kw);
            mMessageLabels.add(kw);
        }
        mMessageTokens.add("rtl");
        mMessageLabels.add("rtl");
        mMessageTokens.add(TOKEN_WRONG_PIN);
        mMessageLabels.add(getString(R.string.phone_sms_dry_wrong_pin));

        renderSegments(mFromGroup, mSenderLabels, mSenderIndex,
                this::selectSender);
        renderSegments(mMessageGroup, mMessageLabels, mMessageIndex,
                this::selectMessage);
    }

    /*** One segmented bar: the selected segment gets the amber tint, the
     *  others stay dim. Views only - no drawable resources needed. */
    private void renderSegments(final LinearLayout group, final List<String> labels,
                                final int selected,
                                final java.util.function.IntConsumer onPick) {
        group.removeAllViews();
        for (int i = 0; i < labels.size(); i++) {
            final int index = i;
            final TextView seg = new TextView(requireContext());
            seg.setText(labels.get(i));
            seg.setGravity(Gravity.CENTER);
            seg.setSingleLine(true);
            seg.setMinWidth(dp(64));
            seg.setPadding(dp(12), 0, dp(12), 0);
            seg.setTextSize(13);
            seg.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
            seg.setOnClickListener(v -> onPick.accept(index));
            group.addView(seg);
        }
        updateSegmentState(group, selected);
    }

    private void updateSegmentState(final LinearLayout group, final int selected) {
        for (int i = 0; i < group.getChildCount(); i++) {
            final TextView seg = (TextView) group.getChildAt(i);
            if (i == selected) {
                final GradientDrawable bg = new GradientDrawable();
                bg.setColor(0x33F4A437);
                bg.setCornerRadius(dp(6));
                bg.setStroke(dp(1), 0x66F4A437);
                seg.setBackground(bg);
                seg.setTextColor(ThemeColors.get(requireContext(), R.attr.deAccentText));
                seg.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                seg.setBackground(null);
                seg.setTextColor(ThemeColors.get(requireContext(), R.attr.deTextDim));
                seg.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }

    private void selectSender(final int index) {
        mSenderIndex = index;
        updateSegmentState(mFromGroup, index);
        evaluateAndRender();
    }

    private void selectMessage(final int index) {
        mMessageIndex = index;
        mEditing = false;
        updateSegmentState(mMessageGroup, index);
        mMessage = buildMessage(mMessageTokens.get(index));
        showEditMode(false);
        updatePreview();
        evaluateAndRender();
    }

    private String buildMessage(final String token) {
        final String pin = Preference.getSmsPin(null);
        if (TOKEN_WRONG_PIN.equals(token)) {
            final String command = mMessageTokens.isEmpty()
                    ? "rtl" : mMessageTokens.get(0);
            /* any PIN that differs from the saved one does the job */
            final String wrongPin = "0000".equals(pin) ? "1111" : "0000";
            return "ATT:" + command + ":" + wrongPin;
        }
        return "ATT:" + token + ":" + pin;
    }

    private void toggleEdit() {
        mEditing = !mEditing;
        showEditMode(mEditing);
        if (mEditing) {
            /* deselect the picks - the message is now free text */
            updateSegmentState(mMessageGroup, -1);
            mMsgEdit.setText(mMessage);
            mMsgEdit.setSelection(mMsgEdit.getText().length());
            mMsgEdit.requestFocus();
        } else {
            mMessage = mMsgEdit.getText().toString();
            updatePreview();
            evaluateAndRender();
        }
    }

    private void showEditMode(final boolean editing) {
        mMsgPreview.setVisibility(editing ? View.GONE : View.VISIBLE);
        mMsgEdit.setVisibility(editing ? View.VISIBLE : View.GONE);
    }

    private void updatePreview() {
        /* the PIN is masked in the preview - the sheet is meant to be
         * shown around other people */
        final int colon = mMessage.lastIndexOf(':');
        mMsgPreview.setText(colon > 0
                ? mMessage.substring(0, colon + 1) + "····" : mMessage);
    }

    ////////// evaluation

    private String currentSender() {
        return mSenderNumbers.get(mSenderIndex);
    }

    private String currentSenderLabel() {
        return mSenderLabels.get(mSenderIndex);
    }

    private void evaluateAndRender() {
        mVerdict = AndruavSMSClientParser.evaluate(
                currentSender(), mMessage,
                System.currentTimeMillis(), gateConfig());
        renderGates();
        renderResult();
    }

    /*** The same preference reads the parser's default provider makes -
     *  duplicated here because that provider is private. */
    private static AndruavSMSClientParser.GateConfigProvider gateConfig() {
        return new AndruavSMSClientParser.GateConfigProvider() {
            @Override public List<String> contacts() {
                return Preference.getSmsAllowedContacts(null);
            }
            @Override public String pin() {
                return Preference.getSmsPin(null);
            }
            @Override public List<String> keywords() {
                return SmsKeywords.parse(Preference.getSmsKeywords(null));
            }
            @Override public long maxAgeS() {
                return Preference.getSmsMaxAgeS(null);
            }
            @Override public boolean receiverOn() {
                return Preference.isSMSRXEnabled(null);
            }
        };
    }

    private void renderGates() {
        mGatesList.removeAllViews();
        for (final SmsGateVerdict.Gate gate : mVerdict.gates) {
            mGatesList.addView(gateRow(gate));
        }
    }

    private View gateRow(final SmsGateVerdict.Gate gate) {
        final LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(9), 0, dp(9));

        final TextView icon = new TextView(requireContext());
        icon.setTextSize(13);
        icon.setMinWidth(dp(24));
        switch (gate.status) {
            case PASS:
                icon.setText("✓");
                icon.setTextColor(ThemeColors.get(requireContext(), R.attr.deGood));
                break;
            case FAIL:
                icon.setText("✕");
                icon.setTextColor(ThemeColors.get(requireContext(), R.attr.deBadText));
                break;
            default:
                icon.setText("–");
                icon.setTextColor(ThemeColors.get(requireContext(), R.attr.deTextDimDarker));
                break;
        }
        row.addView(icon);

        final TextView label = new TextView(requireContext());
        label.setText(gateLabel(gate.name));
        label.setTextColor(ThemeColors.get(requireContext(), R.attr.deTextPrimary));
        label.setTextSize(14);
        label.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(label);

        final TextView detail = new TextView(requireContext());
        detail.setText(gate.detail);
        detail.setTextColor(ThemeColors.get(requireContext(), R.attr.deTextDim));
        detail.setTextSize(11);
        detail.setTypeface(android.graphics.Typeface.MONOSPACE);
        row.addView(detail);

        return row;
    }

    private String gateLabel(final String name) {
        switch (name) {
            case "receiver": return getString(R.string.phone_sms_dry_gate_receiver);
            case "lockout": return getString(R.string.phone_sms_dry_gate_lockout);
            case "sender": return getString(R.string.phone_sms_dry_gate_sender);
            case "format": return getString(R.string.phone_sms_dry_gate_format);
            case "pin": return getString(R.string.phone_sms_dry_gate_pin);
            case "age": return getString(R.string.phone_sms_dry_gate_age,
                    Preference.getSmsMaxAgeS(null));
            case "command": return getString(R.string.phone_sms_dry_gate_command);
            default: return name;
        }
    }

    private void renderResult() {
        final String text;
        if (!mVerdict.allowed) {
            text = getString(R.string.phone_sms_dry_would_refuse, mVerdict.refuseReason);
        } else if (!mVerdict.fact.isEmpty()) {
            text = getString(R.string.phone_sms_dry_would_fire, mVerdict.fact)
                    + "\n{\"from\":\"" + mVerdict.contact + "\",\"age_s\":"
                    + mVerdict.age_s + "}";
        } else {
            text = getString(R.string.phone_sms_dry_would_run, mVerdict.actionText);
        }
        mTxtResult.setText(text);
        mTxtResult.setTextColor(ThemeColors.get(requireContext(),
                mVerdict.allowed ? R.attr.deGood : R.attr.deBadText));
        final GradientDrawable bg = new GradientDrawable();
        bg.setColor(mVerdict.allowed ? 0x2965C67D : 0x29FD736D);
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), mVerdict.allowed ? 0x8065C67D : 0x80FD736D);
        mTxtResult.setBackground(bg);
        mTxtResult.setVisibility(View.VISIBLE);
    }

    ////////// LIVE TEST

    /***
     * The warning dialog (mockup an0133-v2-live): shows the message and
     * sender, lists what a real run does, and blocks behind RUN LIVE.
     * CANCEL runs nothing.
     */
    private void showLiveWarning() {
        final LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundResource(R.drawable.bg_home_card_warning);
        tintStroke(box, ThemeColors.get(requireContext(), R.attr.deBadBorderSoft));
        box.setPadding(dp(20), dp(18), dp(20), dp(16));

        final TextView title = new TextView(requireContext());
        title.setText(R.string.phone_sms_live_title);
        title.setTextColor(ThemeColors.get(requireContext(), R.attr.deTextPrimary));
        title.setTextSize(16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        box.addView(title);

        final LinearLayout msgBox = new LinearLayout(requireContext());
        msgBox.setOrientation(LinearLayout.HORIZONTAL);
        msgBox.setBackgroundResource(R.drawable.bg_home_input);
        msgBox.setPadding(dp(12), dp(10), dp(12), dp(10));
        final LinearLayout.LayoutParams msgLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        msgLp.topMargin = dp(12);
        msgBox.setLayoutParams(msgLp);

        final TextView msgText = new TextView(requireContext());
        final int colon = mMessage.lastIndexOf(':');
        msgText.setText(colon > 0 ? mMessage.substring(0, colon + 1) + "····" : mMessage);
        msgText.setTextColor(ThemeColors.get(requireContext(), R.attr.deTextOnInput));
        msgText.setTextSize(14);
        msgText.setTypeface(android.graphics.Typeface.MONOSPACE);
        msgText.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        msgBox.addView(msgText);

        final TextView fromText = new TextView(requireContext());
        fromText.setText(getString(R.string.phone_sms_live_from, currentSenderLabel()));
        fromText.setTextColor(ThemeColors.get(requireContext(), R.attr.deTextDim));
        fromText.setTextSize(12);
        msgBox.addView(fromText);
        box.addView(msgBox);

        final TextView body = new TextView(requireContext());
        body.setText(R.string.phone_sms_live_body);
        body.setTextColor(ThemeColors.get(requireContext(), R.attr.deTextSecondary));
        body.setTextSize(13);
        final LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = dp(12);
        body.setLayoutParams(bodyLp);
        box.addView(body);

        final TextView bullets = new TextView(requireContext());
        final StringBuilder lines = new StringBuilder();
        if (mVerdict != null && !mVerdict.fact.isEmpty()) {
            lines.append(getString(R.string.phone_sms_live_fact, mVerdict.fact)).append('\n');
        } else if (mVerdict != null && !mVerdict.actionText.isEmpty()) {
            lines.append(getString(R.string.phone_sms_live_command, mVerdict.actionText)).append('\n');
        }
        lines.append(getString(R.string.phone_sms_live_gcs)).append('\n');
        lines.append(getString(R.string.phone_sms_live_reply, currentSenderLabel())).append('\n');
        lines.append(getString(R.string.phone_sms_live_pin));
        bullets.setText(lines.toString());
        bullets.setTextColor(ThemeColors.get(requireContext(), R.attr.deTextSecondary));
        bullets.setTextSize(13);
        final LinearLayout.LayoutParams bulletsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bulletsLp.topMargin = dp(6);
        bullets.setLayoutParams(bulletsLp);
        box.addView(bullets);

        final TextView warn = new TextView(requireContext());
        warn.setText(R.string.phone_sms_live_warn);
        warn.setTextColor(ThemeColors.get(requireContext(), R.attr.deBadText));
        warn.setTextSize(13);
        final LinearLayout.LayoutParams warnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        warnLp.topMargin = dp(12);
        warn.setLayoutParams(warnLp);
        box.addView(warn);

        final LinearLayout buttons = new LinearLayout(requireContext());
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        final LinearLayout.LayoutParams buttonsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonsLp.topMargin = dp(16);
        buttons.setLayoutParams(buttonsLp);

        final TextView btnCancel = dialogButton(
                getString(R.string.phone_sms_live_cancel), false);
        final TextView btnRun = dialogButton(
                getString(R.string.phone_sms_live_run), true);
        buttons.addView(btnCancel);
        buttons.addView(btnRun);
        box.addView(buttons);

        final AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(box).setCancelable(true).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRun.setOnClickListener(v -> {
            dialog.dismiss();
            runLive();
        });
        dialog.show();
    }

    private TextView dialogButton(final String text, final boolean danger) {
        final TextView btn = new TextView(requireContext());
        btn.setText(text);
        btn.setGravity(Gravity.CENTER);
        btn.setTextSize(13);
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, dp(44), 1f);
        btn.setLayoutParams(lp);
        if (danger) {
            lp.setMarginStart(dp(8));
            final GradientDrawable bg = new GradientDrawable();
            bg.setColor(ThemeColors.get(requireContext(), R.attr.deBad));
            bg.setCornerRadius(dp(9));
            btn.setBackground(bg);
            btn.setTextColor(Color.WHITE);
        } else {
            lp.setMarginEnd(dp(8));
            btn.setBackgroundResource(R.drawable.bg_home_btn_outline);
            btn.setTextColor(ThemeColors.get(requireContext(), R.attr.deTextPrimary));
        }
        return btn;
    }

    /***
     * RUN LIVE: the real path with the testRun flag - fact, command, reply
     * SMS and PIN count all happen; every GCS notice is prefixed TEST:.
     */
    private void runLive() {
        new AndruavSMSClientParser().executeCommand(currentSender(), mMessage, true);
        evaluateAndRender();
    }

    private void tintStroke(final View view, final int color) {
        final Drawable bg = view.getBackground();
        if (bg instanceof GradientDrawable) {
            ((GradientDrawable) bg.mutate()).setStroke(dp(1), color);
        }
    }

    private int dp(final int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
