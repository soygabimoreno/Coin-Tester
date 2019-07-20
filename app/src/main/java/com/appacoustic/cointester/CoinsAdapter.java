package com.appacoustic.cointester;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.appacoustic.cointester.CoinsFragment.OnListFragmentInteractionListener;
import com.gabrielmorenoibarra.g.G;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CoinsAdapter extends RecyclerView.Adapter<CoinsAdapter.Holder> {

    private Context context;
    private LayoutInflater inflater;
    private ViewGroup parent;
    private final List<Coin> items;
    private final OnListFragmentInteractionListener listener;

    public CoinsAdapter(List<Coin> items, OnListFragmentInteractionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.parent = parent;
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_coin, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(final Holder holder, int position) {
        Coin coin = items.get(position);
        holder.iv.setImageResource(coin.getHead());
        holder.tvName.setText(coin.getName());
        holder.tvPlace.setText(coin.getPlace());

        G.setAlphaSelector(holder.rlParent, holder.btn);

        holder.rlParent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != listener) {
                    listener.onListFragmentInteraction(items.get(holder.getAdapterPosition()));
                }
            }
        });

        holder.btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                View dialogView = inflater.inflate(R.layout.layout_datasheet, parent, false);
                dialogBuilder.setView(dialogView);

                Coin coin = items.get(holder.getAdapterPosition());

                ((TextView) dialogView.findViewById(R.id.tvDatasheetName)).setText(coin.getName());
                ((TextView) dialogView.findViewById(R.id.tvDatasheetPlace)).setText(coin.getPlace());

                AlertDialog alertDialog = dialogBuilder.create();
                alertDialog.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {

        @BindView(R.id.rlItemCoinParent) RelativeLayout rlParent;
        @BindView(R.id.ivItemCoin) ImageView iv;
        @BindView(R.id.tvItemCoinName) TextView tvName;
        @BindView(R.id.tvItemCoinPlace) TextView tvPlace;
        @BindView(R.id.btnItemCoin) Button btn;

        public Holder(View v) {
            super(v);
            ButterKnife.bind(this, v);
        }
    }
}
