package com.example.hersaferide.ride

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.hersaferide.R

data class SearchSuggestion(
    val mainText: String,
    val secondaryText: String,
    val fullAddress: String,
    val type: SuggestionType = SuggestionType.LOCATION,
    val distance: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

enum class SuggestionType {
    HISTORY, LOCATION, FAVORITE
}

class SearchSuggestionAdapter(
    private var suggestions: List<SearchSuggestion>,
    private val onSuggestionClick: (SearchSuggestion) -> Unit
) : RecyclerView.Adapter<SearchSuggestionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivSuggestionIcon)
        val tvMainText: TextView = view.findViewById(R.id.tvMainText)
        val tvSecondaryText: TextView = view.findViewById(R.id.tvSecondaryText)
        val tvDistance: TextView = view.findViewById(R.id.tvDistance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val suggestion = suggestions[position]
        holder.tvMainText.text = suggestion.mainText
        holder.tvSecondaryText.text = suggestion.secondaryText
        
        // Show distance if available
        if (suggestion.distance != null) {
            holder.tvDistance.visibility = View.VISIBLE
            holder.tvDistance.text = suggestion.distance
        } else {
            holder.tvDistance.visibility = View.GONE
        }

        // Set icon based on type
        val iconRes = when (suggestion.type) {
            SuggestionType.HISTORY -> android.R.drawable.ic_menu_recent_history
            SuggestionType.FAVORITE -> android.R.drawable.btn_star_big_on
            SuggestionType.LOCATION -> android.R.drawable.ic_menu_mylocation
        }
        holder.ivIcon.setImageResource(iconRes)
        
        holder.itemView.setOnClickListener {
            onSuggestionClick(suggestion)
        }
    }

    override fun getItemCount() = suggestions.size

    fun updateSuggestions(newSuggestions: List<SearchSuggestion>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = suggestions.size
            override fun getNewListSize(): Int = newSuggestions.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return suggestions[oldItemPosition].fullAddress == newSuggestions[newItemPosition].fullAddress
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return suggestions[oldItemPosition] == newSuggestions[newItemPosition]
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        suggestions = newSuggestions
        diffResult.dispatchUpdatesTo(this)
    }
}
