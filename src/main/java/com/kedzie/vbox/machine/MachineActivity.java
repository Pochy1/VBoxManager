package com.kedzie.vbox.machine;

import org.virtualbox_4_1.MachineState;
import org.virtualbox_4_1.SessionState;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.kedzie.vbox.BaseListActivity;
import com.kedzie.vbox.MachineTask;
import com.kedzie.vbox.R;
import com.kedzie.vbox.VBoxApplication;
import com.kedzie.vbox.api.IConsole;
import com.kedzie.vbox.api.IMachine;
import com.kedzie.vbox.api.ISnapshot;
import com.kedzie.vbox.api.WebSessionManager;
import com.kedzie.vbox.task.DiscardStateTask;
import com.kedzie.vbox.task.LaunchVMProcessTask;
import com.kedzie.vbox.task.PowerDownTask;
import com.kedzie.vbox.task.ResetTask;
import com.kedzie.vbox.task.ResumeTask;
import com.kedzie.vbox.task.SaveStateTask;
import com.kedzie.vbox.task.TakeSnapshotTask;

public class MachineActivity extends BaseListActivity {
	protected static final String TAG = MachineActivity.class.getSimpleName();
	
	private WebSessionManager _vmgr;
	private IMachine _machine;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _vmgr =getIntent().getParcelableExtra("vmgr");
		_machine = _vmgr.getProxy(IMachine.class, getIntent().getStringExtra("machine"));
		View _headerView = getLayoutInflater().inflate(R.layout.machine_list_item, getListView(), false);
		((ImageView)_headerView.findViewById(R.id.machine_list_item_ostype)).setImageResource(VBoxApplication.get("os_"+_machine.getOSTypeId().toLowerCase()));
		((TextView) _headerView.findViewById(R.id.machine_list_item_name)).setText(_machine.getName()); 
		getListView().addHeaderView(_headerView);
    }
	
	@Override
    public void onDestroy() {
        super.onDestroy();
    }
	
	@Override
	protected void onStart() {
		super.onStart();
		updateState(_machine);
	}
	
	@Override
	protected void onStop() {
		try {
			if(_vmgr.getVBox().getSessionObject().getState().equals(SessionState.Locked)) _vmgr.getVBox().getSessionObject().unlockMachine();
		} catch (Exception e) {
			Log.e(TAG, "Error unlock session", e);
			showAlert(e.toString());
		}
		super.onStop();
	}
	
	private void updateState(IMachine m) {
		MachineState state = m.getState();
		Log.i(TAG, "Update state: " + state);
		((ImageView)getListView().findViewById(R.id.machine_list_item_state)).setImageResource( VBoxApplication.get(state) );
		((TextView)getListView().findViewById(R.id.machine_list_item_state_text)).setText(state.name());
		ISnapshot s = _machine.getCurrentSnapshot();
		if(s!=null)  ((TextView) getListView().findViewById(R.id.machine_list_item_snapshot)).setText("("+s.getName() + ")");		
		setListAdapter(new MachineActionAdapter(this, R.layout.machine_action_item, R.id.action_item_text, R.id.action_item_icon, VBoxApplication.getActions(state)));
		
		getListView().setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,int position, long id) {
				String action = (String)getListView().getAdapter().getItem(position);
				if(action.equals("Start"))	new LaunchVMProcessTask(MachineActivity.this, _vmgr).execute(_machine);
				else if(action.equals("Power Off"))		new PowerDownTask(MachineActivity.this, _vmgr).execute(_machine);
				else if(action.equals("Reset")) 	new ResetTask(MachineActivity.this, _vmgr).execute(_machine);
				else if(action.equals("Pause")) 	
					new MachineTask(MachineActivity.this, _vmgr, "Pausing") {	
						@Override
						protected void work(IMachine m, WebSessionManager vmgr, IConsole console) throws Exception { 
							console.pause();
						}}.execute(_machine);
				else if(action.equals("Resume")) new ResumeTask(MachineActivity.this, _vmgr) .execute(_machine);
				else if(action.equals("Power Button")) 	
					new MachineTask(MachineActivity.this, _vmgr, "ACPI Power Down") {
						@Override
						protected void work(IMachine m, WebSessionManager vmgr, IConsole console) throws Exception {
							console.powerButton();
						}}.execute(_machine);
				else if(action.equals("Save State")) 	new SaveStateTask(MachineActivity.this, _vmgr) .execute(_machine);
				else if(action.equals("Discard State")) 	new DiscardStateTask(MachineActivity.this, _vmgr) .execute(_machine);
				else if(action.equals("Take Snapshot")) 	new TakeSnapshotTask(MachineActivity.this, _vmgr).execute(_machine);
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.machine_options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.machine_option_menu_refresh:
			updateState(_machine);
			return true;
		case R.id.machine_option_menu_metrics:
			Intent intent = new Intent(this, MetricActivity.class);
			intent.putExtra("vmgr", _vmgr);
			intent.putExtra("object", _machine.getId() );
			intent.putExtra("cpuMetrics" , new String[] { "Guest/CPU/Load/User", "Guest/CPU/Load/Kernel" } );
			intent.putExtra("ramMetrics" , new String[] {  "Guest/RAM/Usage/Used" } );
			startActivity(intent);
			return true;
		default:
			return true;
		}
	}
	
	class MachineActionAdapter extends ArrayAdapter<String> {
		private final LayoutInflater _layoutInflater;
		private final int layoutId;
		private final int textResourceId;
		private final int iconResourceId;
		
		public MachineActionAdapter(Context context, int id, int textResourceId, int iconResourceId, String []strings) {
			super(context, id, textResourceId, strings);
			_layoutInflater = LayoutInflater.from(context);
			this.layoutId=id;
			this.textResourceId=textResourceId;
			this.iconResourceId=iconResourceId;
		}

		public View getView(int position, View view, ViewGroup parent) {
			if (view == null) {
				view = _layoutInflater.inflate(this.layoutId, parent, false);
				((TextView)view.findViewById(textResourceId)).setText(getItem(position));
				((ImageView)view.findViewById(iconResourceId)).setImageResource( VBoxApplication.get(getItem(position)));
			}
			return view;
		}
	}

}