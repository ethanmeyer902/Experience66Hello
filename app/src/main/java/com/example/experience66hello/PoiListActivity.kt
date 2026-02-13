package com.example.experience66hello

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class PoiListActivity : ComponentActivity() {

    private lateinit var poiAdapter: PoiAdapter
    private lateinit var allPois: List<Route66Landmark>

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private fun filterList(query: String) {
        val filtered = if (query.isBlank()) {
            allPois
        } else {
            allPois.filter { it.name.contains(query, ignoreCase = true) }
        }
        poiAdapter.submitList(filtered)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load your POIs (use whatever your app uses as the source)
        allPois = ArizonaLandmarks.landmarks   // <-- change this if your source differs

        poiAdapter = PoiAdapter { landmark ->
            // Send selected POI back to MainActivity
            val data = Intent().putExtra("landmark_id", landmark.id)
            setResult(RESULT_OK, data)
            finish()
        }

        // Root layout
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(16.dp(), 16.dp(), 16.dp(), 16.dp())
        }

        // --- Top row: Search + Back button ---
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val searchBar = EditText(this).apply {
            hint = "Search POIs..."
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 10.dp()
            }
            setSingleLine(true)
            setPadding(12.dp(), 10.dp(), 12.dp(), 10.dp())
            setBackgroundColor(Color.parseColor("#F2F2F2"))

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filterList(s?.toString().orEmpty())
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }

        val backBtn = Button(this).apply {
            text = "Back to Map"
            setOnClickListener {
                // No selection, just go back
                setResult(RESULT_CANCELED)
                finish()
            }
        }

        topRow.addView(searchBar)
        topRow.addView(backBtn)
        rootLayout.addView(topRow)

        // Spacer
        rootLayout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 12.dp())
        })

        // --- RecyclerView list ---
        val recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@PoiListActivity)
            adapter = poiAdapter
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        rootLayout.addView(recyclerView)

        setContentView(rootLayout)

        // Initial list
        poiAdapter.submitList(allPois)
    }
}

/** Adapter **/
private class PoiAdapter(
    private val onClick: (Route66Landmark) -> Unit
) : ListAdapter<Route66Landmark, PoiAdapter.VH>(DIFF) {

    class VH(val row: LinearLayout, val title: TextView, val subtitle: TextView) :
        RecyclerView.ViewHolder(row)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val context = parent.context

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 14)
            setBackgroundColor(Color.WHITE)
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
            elevation = 2f
        }

        val title = TextView(context).apply {
            textSize = 16f
            setTextColor(Color.parseColor("#212121"))
        }

        val subtitle = TextView(context).apply {
            textSize = 12f
            setTextColor(Color.parseColor("#757575"))
        }

        row.addView(title)
        row.addView(subtitle)

        return VH(row, title, subtitle)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.title.text = item.name
        holder.subtitle.text = item.id
        holder.row.setOnClickListener { onClick(item) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Route66Landmark>() {
            override fun areItemsTheSame(oldItem: Route66Landmark, newItem: Route66Landmark) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Route66Landmark, newItem: Route66Landmark) =
                oldItem == newItem
        }
    }
}
