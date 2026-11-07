package ap.andruav_ap.activities.phone;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import ap.andruav_ap.R;
import ap.andruav_ap.activities.ThemeColors;

/***
 * Read-only sheet behind the "?" next to Command PIN: the fixed commands
 * {@link ap.andruav_ap.communication.telemetry.AndruavSMSClientParser}
 * accepts inside ATT:<cmd>:<PIN>. The list mirrors the parser switch and
 * SmsKeywords.RESERVED - GOTO is shown dimmed because the parser accepts
 * it but runs nothing for it.
 */
public class SmsCommandsSheet extends BottomSheetDialogFragment {

    public static final String TAG = "SmsCommandsSheet";

    private static final class Command {
        final String syntax;
        @StringRes final int description;
        /** Parser accepts the name but runs nothing - the row is dimmed. */
        final boolean unimplemented;

        Command(final String syntax, @StringRes final int description,
                final boolean unimplemented) {
            this.syntax = syntax;
            this.description = description;
            this.unimplemented = unimplemented;
        }
    }

    private static final Command[] COMMANDS = {
            new Command("HLP", R.string.phone_sms_cmd_hlp, false),
            new Command("RTL", R.string.phone_sms_cmd_rtl, false),
            new Command("LND", R.string.phone_sms_cmd_lnd, false),
            new Command("LOC", R.string.phone_sms_cmd_loc, false),
            new Command("AUTO n", R.string.phone_sms_cmd_auto, false),
            new Command("GOTO", R.string.phone_sms_cmd_goto, true),
    };

    public static SmsCommandsSheet newInstance() {
        return new SmsCommandsSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_sms_commands, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final LinearLayout list = view.findViewById(R.id.phone_sms_cmds_list);
        for (int i = 0; i < COMMANDS.length; i++) {
            list.addView(commandRow(COMMANDS[i]));
            if (i < COMMANDS.length - 1) {
                final View divider = new View(requireContext());
                divider.setBackgroundColor(ThemeColors.get(
                        requireContext(), R.attr.deBorderSoft));
                final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
                lp.topMargin = dp(4);
                divider.setLayoutParams(lp);
                list.addView(divider);
            }
        }
    }

    private View commandRow(final Command command) {
        final LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        final TextView name = new TextView(requireContext());
        name.setText(command.syntax);
        name.setTextColor(ThemeColors.get(requireContext(), command.unimplemented
                ? R.attr.deTextDimDarker : R.attr.deAccentText));
        name.setTextSize(13);
        name.setTypeface(android.graphics.Typeface.MONOSPACE);
        name.setGravity(Gravity.CENTER);
        name.setBackgroundResource(R.drawable.bg_home_tile_gray);
        name.setPadding(dp(10), dp(6), dp(10), dp(6));
        name.setMinWidth(dp(72));
        row.addView(name);

        final TextView desc = new TextView(requireContext());
        desc.setText(command.description);
        desc.setTextColor(ThemeColors.get(requireContext(), command.unimplemented
                ? R.attr.deTextDimDarker : R.attr.deTextPrimary));
        desc.setTextSize(13);
        desc.setPadding(dp(12), 0, 0, 0);
        desc.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(desc);

        return row;
    }

    private int dp(final int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
