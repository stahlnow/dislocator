package com.stahlnow.dislocator;

import java.util.List;

import com.stahlnow.dislocator.Bones.Bone;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class BoneAdapter extends ArrayAdapter<Bone> {
	private final Context context;
	private final List<Bone> bones;

	public BoneAdapter(Context context, List<Bone> bones) {
		super(context, R.layout.boneslist, bones);
		this.context = context;
		this.bones = bones;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.boneslist, parent, false);
		TextView txtName = (TextView) rowView.findViewById(R.id.bonesListName);
		TextView txtDesc = (TextView) rowView.findViewById(R.id.bonesListDescription);
		txtName.setText(bones.get(position).name);
		txtDesc.setText(bones.get(position).description);

				
		return rowView;
	}
}