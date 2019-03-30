package com.ith8.bleconnection.app;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import com.ith8.bleconnection.R;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    private  List<DeviceModel> emptyList;
    ItemClickedListner listner;
    private List<BluetoothDevice> listDevices;

    public DeviceAdapter(List<Object> emptyList, ItemClickedListner listner) {
        this.emptyList = new ArrayList<>();
        this.listDevices = new ArrayList<>();
        this.listner = listner;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
       View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ble,parent,false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder hold, int position) {
        hold.name.setText(emptyList.get(position).getName());
        hold.address.setText(emptyList.get(position).getAddress());
        hold.type.setText(String.valueOf(emptyList.get(position).getRssi()));

    }

    @Override
    public int getItemCount() {
        return emptyList.size();
    }

    public synchronized void setItem(DeviceModel list) {
        boolean contains = false;
//        listDevices.add(devices);
        for (DeviceModel m :
                emptyList) {
            if (m.getAddress().equals(list.getAddress())) {
//                contains=true;
                return;
            }
        }

//        if(!contains)
        emptyList.add(list);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {


        private  TextView name;
        private  TextView address;
        private  TextView type;

        public ViewHolder(@NonNull View itemView) {

            super(itemView);
            name = itemView.findViewById(R.id.name);
            address = itemView.findViewById(R.id.address);
            type = itemView.findViewById(R.id.type);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listner.onItemClicked(getAdapterPosition(), emptyList.get(getAdapterPosition()));
                }
            });

        }
    }

    public interface ItemClickedListner{
         void onItemClicked(int position, DeviceModel model);
    }
}
