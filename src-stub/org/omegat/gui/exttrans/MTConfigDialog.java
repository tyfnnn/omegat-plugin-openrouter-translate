package org.omegat.gui.exttrans;

import javax.swing.*;
import java.awt.Window;

public abstract class MTConfigDialog {

	public final MTConfigPanel panel;
	
	public MTConfigDialog(Window parent, String name) {
        panel = new MTConfigPanel();
	}

	public void show(JPanel configPanel) {
		// TODO Auto-generated method stub
		
	}
	
    protected abstract void onConfirm();

}
