package com.matejdro.pebblenotificationcenter.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.matejdro.pebblenotificationcenter.R;


public class ReplacerEditDialog extends Dialog {

	private ReplacerDialogResult OKListener;
	private ReplacerDialogResult deleteListener;

	private boolean editMode;
	private String existingCharacter;
	private String existingReplacement;
	
	public ReplacerEditDialog(Context context) {
		super(context);
		editMode = false;
	}
	
	public ReplacerEditDialog(Context context, String existingCharacter, String existingReplacement) {
		super(context);
		editMode = true;
		this.existingCharacter = existingCharacter;
		this.existingReplacement = existingReplacement;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.dialog_replacer_edit);
		
		if (editMode)
		{
			setTitle("Edit replacement pair");
			((EditText) findViewById(R.id.editCharacter)).setText(existingCharacter);
			((EditText) findViewById(R.id.editReplacement)).setText(existingReplacement);
		}
		else
		{
			setTitle("Create new pair");
			findViewById(R.id.buttonDelete).setVisibility(View.GONE);
		}
		
		((Button) findViewById(R.id.buttonCancel)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		
		((Button) findViewById(R.id.buttonDelete)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (deleteListener != null)
					deleteListener.dialogFinished(null, null);
				
				dismiss();
			}
		});

		
		((Button) findViewById(R.id.buttonOK)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				EditText characterField = (EditText) findViewById(R.id.editCharacter);
				CharSequence character = characterField.getText();

				if (OKListener != null)
				{
					EditText replacementField = (EditText) findViewById(R.id.editReplacement);
					CharSequence replacement = replacementField.getText();
					
					OKListener.dialogFinished(character, replacement);
				}
				
				dismiss();
			}
		});		
	}
	
	public void setOKListener(ReplacerDialogResult listener)
	{
		OKListener = listener;
	}
	
	public void setDeleteListener(ReplacerDialogResult listener)
	{
		deleteListener = listener;
	}
		
	public static abstract class ReplacerDialogResult
	{
		public abstract void dialogFinished(CharSequence character, CharSequence replacement);
	}
}
