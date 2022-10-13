package org.odk.collect.android.formlists

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import org.odk.collect.android.R
import org.odk.collect.android.databinding.SortItemLayoutBinding
import org.odk.collect.androidshared.system.ContextUtils.getThemeAttributeValue

class FormListSortingAdapter(
    private val sortingOptions: List<FormListSortingOption>,
    private val selectedSortingOrder: Int,
    private val listener: (position: Int) -> Unit
) : RecyclerView.Adapter<FormListSortingAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SortItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            holder.binding.root.setOnClickListener {
                listener.invoke(position)
                selectItem(holder.binding)
            }

            with(sortingOptions[position]) {
                binding.title.setText(this.text)
                binding.icon.setImageResource(this.icon)
                binding.icon.tag = this.icon
                binding.icon.setImageDrawable(
                    DrawableCompat.wrap(binding.icon.drawable).mutate()
                )

                if (position == selectedSortingOrder) {
                    selectItem(binding)
                }
            }
        }
    }

    private fun selectItem(binding: SortItemLayoutBinding) {
        binding.title.setTextColor(getThemeAttributeValue(binding.root.context, R.attr.colorAccent))
        DrawableCompat.setTintList(
            binding.icon.drawable,
            ColorStateList.valueOf(getThemeAttributeValue(binding.root.context, R.attr.colorAccent))
        )
    }

    override fun getItemCount() = sortingOptions.size

    class ViewHolder(val binding: SortItemLayoutBinding) : RecyclerView.ViewHolder(binding.root)
}
