package com.stahlnow.dislocator;

import com.stahlnow.dislocator.Bones.Bone;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.AdapterView.OnItemClickListener;

public class BonesListActivity extends ListActivity {
	
	private BoneAdapter mAdapter;
	
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		mAdapter = new BoneAdapter(this, Bones.BONES);
		
		setListAdapter(mAdapter);
		
		getListView().setOnItemClickListener(new OnItemClickListener() {
			
			@Override
			public void onItemClick(AdapterView<?> parent, final View view, final int position, long id) {
		        
				final Bone item = (Bone) parent.getItemAtPosition(position);

				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder( BonesListActivity.this );

				// set dialog message
				alertDialogBuilder
						.setTitle("Do you really want to remove this bone?")
						.setCancelable(false)
						.setPositiveButton("OK",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										
										 view.animate().setDuration(500).alpha(0)
								            .withEndAction(new Runnable() {
								              @Override
								              public void run() {
								            	  Bones.removeItem(item);
								            	  mAdapter.notifyDataSetChanged();
								              }
								            });
										
									}
								})
						.setNegativeButton("Cancel",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										dialog.cancel();
									}
								});

				// create alert dialog
				AlertDialog alertDialog = alertDialogBuilder.create();

				// show it
				alertDialog.show();
			}
		});

	}
}