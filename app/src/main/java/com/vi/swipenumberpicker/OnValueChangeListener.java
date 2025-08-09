package com.vi.swipenumberpicker;

/**
 * Created by Vitalii.Ishchuk on 25-Jun-15
 */
public interface OnValueChangeListener {
	/**
	 * Notify the listener with new value
	 *
	 * @param view     SwipeNumberPicker
	 * @param oldValue the primary value
	 * @param newValue changed value with dialog or swipe
	 * @return true if the new value can be selected, otherwise primary value will be selected.
	 */
	boolean onValueChange(SwipeNumberPicker view, int oldValue, int newValue);
}
