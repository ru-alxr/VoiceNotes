package mx.alxr.voicenotes.utils.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import androidx.annotation.Nullable;

public class CheckableImageView extends androidx.appcompat.widget.AppCompatImageView implements Checkable {

    public CheckableImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckableImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private boolean mChecked;

    @Override
    public void setChecked(boolean checked) {
        mChecked = checked;
        refreshDrawableState();
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void toggle() {
        setChecked(!mChecked);
    }

    private static final int[] CheckedStateSet = {
            android.R.attr.state_checked,
    };

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CheckedStateSet);
        }
        return drawableState;
    }

}