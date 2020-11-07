package com.appacoustic.cointester.libbase.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

fun <T, VH : ListAdapterBuilder.ViewHolder<T>> createListAdapter(
    init: ListAdapterBuilder<T, VH>.() -> Unit
): ListAdapter<T, VH> = ListAdapterBuilder(init).build()

class ListAdapterBuilder<T, VH : ListAdapterBuilder.ViewHolder<T>>() {
    constructor(init: ListAdapterBuilder<T, VH>.() -> Unit) : this() {
        init()
    }

    @LayoutRes
    private var layoutRes: Int? = null
    private var areItemsEqualByReference: ((T, T) -> Boolean)? = null
    private var areItemsEqualByContent: ((T, T) -> Boolean)? = null
    private var viewHolderCreation: ((View, Int) -> VH)? = null
    private var itemViewType: (Int, Int) -> Int = { _, _ -> layoutRes ?: 1 }

    fun layout(init: () -> Int) {
        layoutRes = init()
    }

    fun compareItemsByReference(f: (T, T) -> Boolean) {
        areItemsEqualByReference = f
    }

    fun compareItemsByContent(f: (T, T) -> Boolean) {
        areItemsEqualByContent = f
    }

    fun viewHolderCreation(f: (View, Int) -> VH) {
        viewHolderCreation = f
    }

    fun viewTypeByPosition(f: (Int, Int) -> Int) {
        itemViewType = f
    }

    fun build(): ListAdapter<T, VH> {
        val layout = requireNotNull(layoutRes)
        val byReference = requireNotNull(areItemsEqualByReference)
        val byContent = requireNotNull(areItemsEqualByContent)
        val createViewHolder = requireNotNull(viewHolderCreation)

        val diffCalculator = object : DiffUtil.ItemCallback<T>() {
            override fun areItemsTheSame(
                oldItem: T,
                newItem: T
            ): Boolean =
                byReference(
                    oldItem,
                    newItem
                )

            override fun areContentsTheSame(
                oldItem: T,
                newItem: T
            ): Boolean =
                byContent(
                    oldItem,
                    newItem
                )
        }

        return object : ListAdapter<T, VH>(diffCalculator) {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): VH =
                LayoutInflater.from(parent.context)
                    .inflate(
                        layout,
                        parent,
                        false
                    )
                    .run {
                        createViewHolder(
                            this,
                            viewType
                        )
                    }

            override fun onBindViewHolder(
                holder: VH,
                position: Int
            ) {
                holder.bind(getItem(position))
            }

            override fun getItemViewType(position: Int): Int {
                return itemViewType(
                    position,
                    itemCount
                )
            }
        }
    }

    abstract class ViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(item: T)
    }
}
