package ap.andruav_ap.activities.phone;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
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
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.ChipGroup;

import java.util.List;
import java.util.Locale;

import ap.andruav_ap.R;
import ap.andruav_ap.activities.AppThemes;
import ap.andruav_ap.activities.ThemeColors;
import ap.andruav_ap.communication.telemetry.SmsKeywords;
import ap.andruavmiddlelibrary.preference.Preference;

/***
 * AN-0138 (TASK-117): SMS tab of the PHONE screen - structured editors that
 * replace the five comma-separated SMS fields of Settings. Every control
 * reads and writes the same preferences (key_sms_rx_*, key_sms_chk,
 * key_mobile_recovery), so the tab and Settings always agree. Contacts show
 * masked numbers - the full number stays in the preference.
 */
public class PhoneSmsFragment extends Fragment {

    /** Stepper range for key_sms_rx_max_age_s - under the minimum the age
     *  check loses meaning, over the maximum it accepts very old SMS. */
    private static final long AGE_MIN_S = 30L;
    private static final long AGE_MAX_S = 3600L;
    private static final long AGE_STEP_S = 30L;

    private TextView mTglCommands;
    private TextView mTxtContactsCount;
    private LinearLayout mContactsList;
    private View mPinBox;
    private EditText mEdtPin;
    private TextView mBtnPinShow;
    private TextView mTxtAge;
    private ChipGroup mKeywordsGroup;
    private TextView mTglInfo;
    private TextView mTxtInfoTarget;
    private TextView mBtnDryRun;

    private boolean mPinShown;
    private boolean mUpdatingPin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_phone_sms, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTglCommands = view.findViewById(R.id.phone_sms_commands_toggle);
        mTxtContactsCount = view.findViewById(R.id.phone_sms_contacts_count);
        mContactsList = view.findViewById(R.id.phone_sms_contacts_list);
        mPinBox = view.findViewById(R.id.phone_sms_pin_box);
        mEdtPin = view.findViewById(R.id.phone_sms_pin);
        mBtnPinShow = view.findViewById(R.id.phone_sms_pin_show);
        mTxtAge = view.findViewById(R.id.phone_sms_age_value);
        mKeywordsGroup = view.findViewById(R.id.phone_sms_keywords);
        mTglInfo = view.findViewById(R.id.phone_sms_info_toggle);
        mTxtInfoTarget = view.findViewById(R.id.phone_sms_info_target);
        mBtnDryRun = view.findViewById(R.id.phone_sms_btn_dry_run);

        mTglCommands.setOnClickListener(v -> {
            Preference.isSMSRXEnabled(null, !Preference.isSMSRXEnabled(null));
            updateToggles();
        });
        mTglInfo.setOnClickListener(v -> {
            Preference.isSMSTXEnabled(null, !Preference.isSMSTXEnabled(null));
            updateToggles();
        });
        view.findViewById(R.id.phone_sms_btn_add_contact)
                .setOnClickListener(v -> openContactSheet());
        view.findViewById(R.id.phone_sms_info_text)
                .setOnClickListener(v -> openTargetSheet());
        view.findViewById(R.id.phone_sms_age_minus)
                .setOnClickListener(v -> stepAge(-AGE_STEP_S));
        view.findViewById(R.id.phone_sms_age_plus)
                .setOnClickListener(v -> stepAge(AGE_STEP_S));

        /* AN-0139: the dry-run sheet - evaluate() only, no SMS leaves the
         * phone; LIVE TEST inside the sheet is the guarded real path */
        mBtnDryRun.setOnClickListener(v -> {
            if (isAdded()) {
                SmsDryRunSheet.newInstance()
                        .show(getParentFragmentManager(), SmsDryRunSheet.TAG);
            }
        });

        mBtnPinShow.setOnClickListener(v -> togglePinVisibility());
        mEdtPin.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                /* write-through: the tab and Settings share key_sms_rx_pin */
                if (!mUpdatingPin) {
                    Preference.setSmsPin(null, s.toString());
                    updatePinState();
                }
            }
        });

        tintDryRunButton();
        reload();
    }

    private void tintDryRunButton() {
        /* the outline drawable's gray stroke becomes amber for the mockup's
         * highlighted DRY RUN look */
        final Drawable bg = mBtnDryRun.getBackground().mutate();
        if (bg instanceof RippleDrawable) {
            final Drawable inner = ((RippleDrawable) bg).getDrawable(0);
            if (inner instanceof GradientDrawable) {
                final int px = (int) (1.5f * getResources().getDisplayMetrics().density + 0.5f);
                ((GradientDrawable) inner).setStroke(px,
                        ThemeColors.get(requireContext(), R.attr.deAccent));
            }
        }
    }

    ////////// load + render

    private void reload() {
        updateToggles();
        renderContacts();
        renderPin();
        renderAge();
        renderKeywords();
        renderInfoTarget();
    }

    private void updateToggles() {
        applyToggle(mTglCommands, Preference.isSMSRXEnabled(null));
        applyToggle(mTglInfo, Preference.isSMSTXEnabled(null));
        updatePinState();
    }

    /***
     * An empty PIN fails the gate's receiver check - every command is
     * refused even with contacts set. While SMS Commands is on, the PIN
     * box gets the red border the sheet uses for invalid fields.
     */
    private void updatePinState() {
        final boolean missing = Preference.isSMSRXEnabled(null)
                && Preference.getSmsPin(null).isEmpty();
        tintStroke(mPinBox, ThemeColors.get(requireContext(),
                missing ? R.attr.deBadText : R.attr.deBorderInput));
    }

    private void tintStroke(final View view, final int color) {
        final Drawable bg = view.getBackground();
        if (bg instanceof GradientDrawable) {
            ((GradientDrawable) bg.mutate()).setStroke(dp(1), color);
        }
    }

    private void applyToggle(final TextView toggle, final boolean on) {
        toggle.setText(AppThemes.switchLabel(requireContext(), on));
        toggle.setBackgroundResource(on
                ? R.drawable.bg_home_bt_toggle_on : R.drawable.bg_home_bt_toggle_off);
    }

    private void renderContacts() {
        final List<String> contacts = Preference.getSmsAllowedContacts(null);
        mTxtContactsCount.setText(String.valueOf(contacts.size()));
        mContactsList.removeAllViews();
        if (contacts.isEmpty()) {
            final TextView empty = new TextView(requireContext());
            empty.setText(R.string.phone_sms_contacts_empty);
            /* red while commands are on - an empty list refuses every sender */
            empty.setTextColor(ThemeColors.get(requireContext(),
                    Preference.isSMSRXEnabled(null)
                            ? R.attr.deBadText : R.attr.deTextDimDarker));
            empty.setTextSize(11);
            mContactsList.addView(empty);
            return;
        }
        for (int i = 0; i < contacts.size(); i++) {
            mContactsList.addView(contactRow(contacts.get(i), i));
            if (i < contacts.size() - 1) {
                final View divider = new View(requireContext());
                divider.setBackgroundColor(ThemeColors.get(
                        requireContext(), R.attr.deBorderSoft));
                final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
                lp.topMargin = dp(4);
                divider.setLayoutParams(lp);
                mContactsList.addView(divider);
            }
        }
    }

    private View contactRow(final String entry, final int index) {
        final String name = SmsKeywords.entryName(entry);
        final String number = SmsKeywords.entryNumber(entry);

        final LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final TextView avatar = new TextView(requireContext());
        avatar.setBackgroundResource(R.drawable.bg_home_icon_tile);
        avatar.setGravity(Gravity.CENTER);
        avatar.setTextColor(ThemeColors.get(requireContext(),
                name.isEmpty() ? R.attr.deTextDim : R.attr.deAccentText));
        avatar.setTextSize(12);
        avatar.setText(initials(name));
        avatar.setTypeface(null, android.graphics.Typeface.BOLD);
        final LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(dp(38), dp(38));
        avatarLp.setMarginEnd(dp(12));
        avatar.setLayoutParams(avatarLp);
        row.addView(avatar);

        final LinearLayout texts = new LinearLayout(requireContext());
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final TextView txtName = new TextView(requireContext());
        txtName.setText(name.isEmpty() ? maskNumber(number) : name);
        txtName.setTextColor(ThemeColors.get(requireContext(), R.attr.deTextPrimary));
        txtName.setTextSize(14);
        texts.addView(txtName);
        if (!name.isEmpty()) {
            final TextView txtNumber = new TextView(requireContext());
            txtNumber.setText(maskNumber(number));
            txtNumber.setTextColor(ThemeColors.get(requireContext(), R.attr.deTextDim));
            txtNumber.setTextSize(11);
            txtNumber.setTypeface(android.graphics.Typeface.MONOSPACE);
            texts.addView(txtNumber);
        }
        row.addView(texts);

        final TextView btnRemove = new TextView(requireContext());
        btnRemove.setText("✕");
        btnRemove.setTextColor(ThemeColors.get(requireContext(), R.attr.deTextDim));
        btnRemove.setTextSize(14);
        btnRemove.setGravity(Gravity.CENTER);
        btnRemove.setPadding(dp(8), dp(4), dp(8), dp(4));
        btnRemove.setOnClickListener(v -> removeContact(index));
        row.addView(btnRemove);

        return row;
    }

    private void removeContact(final int index) {
        final List<String> contacts = Preference.getSmsAllowedContacts(null);
        if (index < 0 || index >= contacts.size()) return;
        contacts.remove(index);
        Preference.setSmsAllowedContacts(null, joinCsv(contacts));
        renderContacts();
    }

    private void openContactSheet() {
        if (!isAdded()) return;
        SmsContactSheet.addContact(Preference.getSmsAllowedContacts(null),
                (mode, name, number) -> {
                    final List<String> contacts = Preference.getSmsAllowedContacts(null);
                    contacts.add(name + "=" + number);
                    Preference.setSmsAllowedContacts(null, joinCsv(contacts));
                    renderContacts();
                    return true;
                }).show(getParentFragmentManager(), SmsContactSheet.TAG);
    }

    private void openTargetSheet() {
        if (!isAdded()) return;
        SmsContactSheet.editTarget(Preference.getRecoveryPhoneNo(null),
                (mode, name, number) -> {
                    Preference.setRecoveryPhoneNo(null, number);
                    renderInfoTarget();
                    return true;
                }).show(getParentFragmentManager(), SmsContactSheet.TAG);
    }

    private void openKeywordSheet() {
        if (!isAdded()) return;
        SmsContactSheet.addKeyword(currentKeywords(),
                (mode, keyword, unused) -> {
                    final List<String> keywords = currentKeywords();
                    keywords.add(keyword);
                    Preference.setSmsKeywords(null, joinCsv(keywords));
                    renderKeywords();
                    return true;
                }).show(getParentFragmentManager(), SmsContactSheet.TAG);
    }

    private void removeKeyword(final String keyword) {
        final List<String> keywords = currentKeywords();
        keywords.remove(keyword);
        Preference.setSmsKeywords(null, joinCsv(keywords));
        renderKeywords();
    }

    private List<String> currentKeywords() {
        /* parse() drops reserved/invalid entries; writing the parsed list
         * back keeps the stored CSV clean */
        return SmsKeywords.parse(Preference.getSmsKeywords(null));
    }

    private void renderKeywords() {
        mKeywordsGroup.removeAllViews();
        for (final String keyword : currentKeywords()) {
            mKeywordsGroup.addView(keywordChip(keyword));
        }
        final TextView add = new TextView(requireContext());
        add.setText(R.string.phone_sms_keyword_add);
        add.setTextColor(ThemeColors.get(requireContext(), R.attr.deAccentText));
        add.setTextSize(12);
        add.setTypeface(null, android.graphics.Typeface.BOLD);
        add.setGravity(Gravity.CENTER);
        add.setPadding(dp(14), dp(7), dp(14), dp(7));
        add.setBackgroundResource(R.drawable.bg_home_btn_outline);
        add.setOnClickListener(v -> openKeywordSheet());
        mKeywordsGroup.addView(add);
    }

    private View keywordChip(final String keyword) {
        final LinearLayout chip = new LinearLayout(requireContext());
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setBackgroundResource(R.drawable.bg_home_tile_gray);
        chip.setPadding(dp(12), dp(6), dp(8), dp(6));

        final TextView label = new TextView(requireContext());
        label.setText(keyword);
        label.setTextColor(ThemeColors.get(requireContext(), R.attr.deTextPrimary));
        label.setTextSize(13);
        chip.addView(label);

        final TextView btnRemove = new TextView(requireContext());
        btnRemove.setText("✕");
        btnRemove.setTextColor(ThemeColors.get(requireContext(), R.attr.deTextDim));
        btnRemove.setTextSize(11);
        btnRemove.setPadding(dp(8), 0, 0, 0);
        btnRemove.setOnClickListener(v -> removeKeyword(keyword));
        chip.addView(btnRemove);

        return chip;
    }

    private void renderPin() {
        mUpdatingPin = true;
        mEdtPin.setText(Preference.getSmsPin(null));
        mEdtPin.setSelection(mEdtPin.getText().length());
        mUpdatingPin = false;
    }

    private void togglePinVisibility() {
        mPinShown = !mPinShown;
        mEdtPin.setInputType(mPinShown
                ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL
                : InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        mEdtPin.setSelection(mEdtPin.getText().length());
        mBtnPinShow.setText(mPinShown ? R.string.phone_sms_pin_hide : R.string.phone_sms_pin_show);
    }

    private void stepAge(final long deltaS) {
        long age = Preference.getSmsMaxAgeS(null) + deltaS;
        age = Math.max(AGE_MIN_S, Math.min(AGE_MAX_S, age));
        Preference.setSmsMaxAgeS(null, age);
        renderAge();
    }

    private void renderAge() {
        mTxtAge.setText(String.valueOf(Preference.getSmsMaxAgeS(null)));
    }

    private void renderInfoTarget() {
        final String target = Preference.getRecoveryPhoneNo(null);
        if (target.isEmpty()) {
            mTxtInfoTarget.setText(R.string.phone_sms_info_no_target);
        } else {
            mTxtInfoTarget.setText(getString(R.string.phone_sms_info_target, maskNumber(target)));
        }
    }

    ////////// helpers

    /*** "+966501234567" -> "+966 50 ···· 567" - the tab never shows a full
     *  number on screen, only in the edit sheet. */
    static String maskNumber(final String number) {
        if (number == null) return "";
        final String digits = number.replaceAll("[^0-9]", "");
        if (digits.length() <= 6) return number;
        final String cc = digits.substring(0, Math.min(3, digits.length() - 4));
        final String head = digits.length() > 5 ? digits.substring(3, 5) : "";
        return "+" + cc + " " + head + " ···· " + digits.substring(digits.length() - 3);
    }

    private static String initials(final String name) {
        if (name == null || name.isEmpty()) return "·";
        return name.length() > 1
                ? name.substring(0, 2).toUpperCase(Locale.US)
                : name.toUpperCase(Locale.US);
    }

    private static String joinCsv(final List<String> items) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    private int dp(final int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onResume() {
        super.onResume();
        /* Settings edits land on the same keys - reloading on resume keeps
         * the two screens in step */
        reload();
    }
}
