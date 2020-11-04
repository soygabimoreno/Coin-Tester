package com.appacoustic.cointester.aaa

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.appacoustic.cointester.R
import com.appacoustic.cointester.coredomain.Coin
import com.appacoustic.cointester.presentation.CoinsFragment.OnListFragmentInteractionListener
import com.gabrielmorenoibarra.g.G

class CoinsAdapter(
    private val items: List<Coin>,
    private val listener: OnListFragmentInteractionListener?
) : RecyclerView.Adapter<CoinsAdapter.Holder>() {
    private var context: Context? = null
    private var inflater: LayoutInflater? = null
    private var parent: ViewGroup? = null
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder {
        context = parent.context
        inflater = context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        this.parent = parent
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_coin,
            parent,
            false
        )
        return Holder(view)
    }

    override fun onBindViewHolder(
        holder: Holder,
        position: Int
    ) {
        val (name, place, head) = items[position]
        holder.ivItemCoin!!.setImageResource(head)
        holder.tvItemCoinName!!.text = name
        holder.tvItemCoinPlace!!.text = place
        G.setAlphaSelector(
            holder.rlItemCoinParent,
            holder.btnItemCoin
        )
        holder.rlItemCoinParent!!.setOnClickListener { listener?.onListFragmentInteraction(items[holder.adapterPosition]) }
        holder.btnItemCoin!!.setOnClickListener {
            val dialogBuilder = AlertDialog.Builder(context!!)
            val dialogView = inflater!!.inflate(
                R.layout.layout_datasheet,
                parent,
                false
            )
            dialogBuilder.setView(dialogView)
            val (name1, place1, head1) = items[holder.adapterPosition]
            (dialogView.findViewById<View>(R.id.tvDatasheetName) as TextView).text = name
            (dialogView.findViewById<View>(R.id.tvDatasheetPlace) as TextView).text = place
            val alertDialog = dialogBuilder.create()
            alertDialog.show()
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class Holder(v: View?) : RecyclerView.ViewHolder(v!!) {
        var rlItemCoinParent: RelativeLayout? = null
        var ivItemCoin: ImageView? = null
        var tvItemCoinName: TextView? = null
        var tvItemCoinPlace: TextView? = null
        var btnItemCoin: Button? = null

        init {
            rlItemCoinParent = v!!.findViewById(R.id.rlItemCoinParent)
            ivItemCoin = v.findViewById(R.id.ivItemCoin)
            tvItemCoinName = v.findViewById(R.id.tvItemCoinName)
            tvItemCoinPlace = v.findViewById(R.id.tvItemCoinPlace)
            btnItemCoin = v.findViewById(R.id.btnItemCoin)
        }
    }
}
