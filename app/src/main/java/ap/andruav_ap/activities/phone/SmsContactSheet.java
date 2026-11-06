package ap.andruav_ap.activities.phone;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import ap.andruav_ap.R;
import ap.andruav_ap.activities.ThemeColors;

/***
 * AN-0138 (TASK-117): the dark bottom sheet behind the SMS tab's "+ ADD"
 * actions, in the sheet_fcb_connection style. Three modes share the layout:
 * ADD CONTACT (name + international number), SMS INFO TARGET (number only)
 * and ADD KEYWORD (the name field relabeled). Validation runs on every
 * keystroke through {@link SmsEntryValidator}; SAVE stays disabled while a
 * field has an error.
 */
public class SmsContactSheet extends BottomSheetDialogFragment {

    public static final String TAG = "SmsContactSheet";

    public static final int MODE_ADD_CONTACT = 0;
    public static final int MODE_TARGET = 1;
    public static final int MODE_KEYWORD = 2;

    /*** Return true to dismiss the sheet after the value is stored. */
    public interface SaveHandler {
        boolean onSave(int mode, String name, String number);
    }

    private int mMode = MODE_ADD_CONTACT;
    private List<String> mExisting = new ArrayList<>();
    private String mPrefill = "";
    private SaveHandler mSaveHandler;

    private TextView mTxtTitle;
    private View mGroupName;
    private TextView mLblName;
    private EditText mEdtName;
    private TextView mErrName;
    private View mGroupNumber;
    private EditText mEdtNumber;
    private TextView mErrNumber;
    private TextView mBtnSave;

    public static SmsContactSheet addContact(final List<String> existingEntries,
                                             final SaveHandler handler) {
        final SmsContactSheet sheet = new SmsContactSheet();
        sheet.mMode = MODE_ADD_CONTACT;
        sheet.mExisting = existingEntries != null ? existingEntries : new ArrayList<>();
        sheet.mSaveHandler = handler;
        return sheet;
    }

    public static SmsContactSheet editTarget(final String currentNumber,
                                             final SaveHandler handler) {
        final SmsContactSheet sheet = new SmsContactSheet();
        sheet.mMode = MODE_TARGET;
        sheet.mPrefill = currentNumber != null ? currentNumber : "";
        sheet.mSaveHandler = handler;
        return sheet;
    }

    public static SmsContactSheet addKeyword(final List<String> existingKeywords,
                                             final SaveHandler handler) {
        final SmsContactSheet sheet = new SmsContactSheet();
        sheet.mMode = MODE_KEYWORD;
        sheet.mExisting = existingKeywords != null ? existingKeywords : new ArrayList<>();
        sheet.mSaveHandler = handler;
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_sms_contact, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTxtTitle = view.findViewById(R.id.sms_sheet_title);
        mGroupName = view.findViewById(R.id.sms_sheet_group_name);
        mLblName = view.findViewById(R.id.sms_sheet_lbl_name);
        mEdtName = view.findViewById(R.id.sms_sheet_edt_name);
        mErrName = view.findViewById(R.id.sms_sheet_err_name);
        mGroupNumber = view.findViewById(R.id.sms_sheet_group_number);
        mEdtNumber = view.findViewById(R.id.sms_sheet_edt_number);
        mErrNumber = view.findViewById(R.id.sms_sheet_err_number);
        mBtnSave = view.findViewById(R.id.sms_sheet_btn_save);
        final TextView btnCancel = view.findViewById(R.id.sms_sheet_btn_cancel);

        switch (mMode) {
            case MODE_TARGET:
                mTxtTitle.setText(R.string.sheet_sms_target_title);
                mGroupName.setVisibility(View.GONE);
                mEdtNumber.setText(mPrefill);
                break;
            case MODE_KEYWORD:
                mTxtTitle.setText(R.string.sheet_sms_keyword_title);
                mLblName.setText(R.string.sheet_sms_keyword_label);
                mGroupNumber.setVisibility(View.GONE);
                break;
            default:
                mTxtTitle.setText(R.string.sheet_sms_add_contact_title);
                break;
        }

        final TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                updateFields();
            }
        };
        mEdtName.addTextChangedListener(watcher);
        mEdtNumber.addTextChangedListener(watcher);

        btnCancel.setOnClickListener(v -> dismiss());
        mBtnSave.setOnClickListener(v -> save());

        updateFields();
    }

    private SmsEntryValidator.Result nameResult() {
        final String raw = mEdtName.getText().toString();
        return mMode == MODE_KEYWORD
                ? SmsEntryValidator.keyword(raw, mExisting)
                : SmsEntryValidator.contactName(raw, mExisting);
    }

    private SmsEntryValidator.Result numberResult() {
        final String raw = mEdtNumber.getText().toString();
        return mMode == MODE_TARGET
                ? SmsEntryValidator.phoneNumber(raw)
                : SmsEntryValidator.phoneNumber(raw, mExisting);
    }

    private void updateFields() {
        boolean valid = true;
        if (mGroupName.getVisibility() == View.VISIBLE) {
            final SmsEntryValidator.Result r = nameResult();
            showFieldState(mEdtName, mErrName, r, R.string.sheet_sms_name_hint, true);
            valid = r.ok;
        }
        if (mGroupNumber.getVisibility() == View.VISIBLE) {
            final SmsEntryValidator.Result r = numberResult();
            showFieldState(mEdtNumber, mErrNumber, r, 0, false);
            valid = valid && r.ok;
        }
        mBtnSave.setEnabled(valid);
        mBtnSave.setAlpha(valid ? 1f : 0.4f);
    }

    /***
     * Empty fields show the dim helper (or nothing) and block SAVE quietly;
     * a non-empty invalid field gets the red error text and a red border.
     */
    private void showFieldState(final EditText edt, final TextView errView,
                                final SmsEntryValidator.Result r,
                                final int helperRes, final boolean keepHelper) {
        final boolean empty = edt.getText().toString().trim().isEmpty();
        if (!empty && !r.ok) {
            errView.setText(errorText(r.error));
            errView.setTextColor(ThemeColors.get(requireContext(), R.attr.deBadText));
            errView.setVisibility(View.VISIBLE);
            tintBorder(edt, ThemeColors.get(requireContext(), R.attr.deBadText));
        } else {
            tintBorder(edt, ThemeColors.get(requireContext(), R.attr.deBorderInput));
            if (keepHelper) {
                errView.setText(helperRes);
                errView.setTextColor(ThemeColors.get(requireContext(), R.attr.deTextDimDarker));
                errView.setVisibility(View.VISIBLE);
            } else {
                errView.setVisibility(View.GONE);
            }
        }
    }

    private void tintBorder(final View view, final int color) {
        final Drawable bg = view.getBackground();
        if (bg instanceof GradientDrawable) {
            final int px = (int) (getResources().getDisplayMetrics().density + 0.5f);
            ((GradientDrawable) bg.mutate()).setStroke(px, color);
        }
    }

    private int errorText(final SmsEntryValidator.Error error) {
        switch (error) {
            case EMPTY: return R.string.sheet_sms_err_required;
            case RESERVED: return R.string.sheet_sms_err_reserved;
            case DUPLICATE: return R.string.sheet_sms_err_duplicate;
            case TOO_LONG: return R.string.sheet_sms_err_long;
            case NUMBER_FORMAT:
            case NUMBER_LENGTH: return R.string.sheet_sms_err_number;
            default: return R.string.sheet_sms_err_chars;
        }
    }

    private void save() {
        if (mSaveHandler == null) {
            dismiss();
            return;
        }
        final String name = nameResult().value;
        final String number = mGroupNumber.getVisibility() == View.VISIBLE
                ? numberResult().value : "";
        if (mSaveHandler.onSave(mMode, name, number)) {
            dismiss();
        }
    }
}
