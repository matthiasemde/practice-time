package de.practicetime.practicetime

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Dao
import android.view.ViewGroup.LayoutParams
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import de.practicetime.practicetime.entities.Category
import kotlinx.coroutines.launch


/**
 *  Adapter for the Category selection button grid.
 */

class CategoryAdapter(
    private val categories: ArrayList<Category>,
    private val context: Activity,
    private val showInActiveSession: Boolean = false,
    private val shortClickHandler: (category: Category, categoryView: View) -> Unit = { _, _ -> },
    private val longClickHandler: (categoryId: Int, categoryView: View) -> Boolean = { _, _ -> false },
    private val addCategoryHandler: () -> Unit = {},
    ) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    init {

    }

    companion object {
        private const val VIEW_TYPE_CATEGORY = 1
        private const val VIEW_TYPE_ADD_NEW = 2
    }

    // returns the view type (ADD_NEW button on last position)
    override fun getItemViewType(position: Int): Int {
        return if (position < categories.size)
            VIEW_TYPE_CATEGORY
        else
            VIEW_TYPE_ADD_NEW
    }

    // return the amount of categories (+1 for the add new button if shown in active session )
    override fun getItemCount() = categories.size + if(showInActiveSession) 1 else 0

    // create new views depending on view type
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)
        return when (viewType) {
            VIEW_TYPE_CATEGORY -> ViewHolder.CategoryViewHolder(
                inflater.inflate(
                    R.layout.view_category_item,
                    viewGroup,
                    false
                ),
                showInActiveSession,
                context,
                shortClickHandler,
                longClickHandler,
            )
            else -> ViewHolder.AddNewCategoryViewHolder(
                inflater.inflate(
                    R.layout.view_add_new_category,
                    viewGroup,
                    false
                ),
                addCategoryHandler,
            )
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        when (viewHolder) {
            is ViewHolder.CategoryViewHolder -> viewHolder.bind(
                categories[position]
            )
            is ViewHolder.AddNewCategoryViewHolder -> viewHolder.bind()
        }
    }

    sealed class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        class CategoryViewHolder(
            view: View,
            showInActiveSession: Boolean,
            private val context: Activity,
            private val shortClickHandler: (category: Category, categoryView: View) -> Unit,
            private val longClickHandler: (categoryId: Int, categoryView: View) -> Boolean,
        ) : ViewHolder(view) {
            private val button: Button = view.findViewById(R.id.categoryButton)

            init {
                // if the category is not shown inside the active session
                // it can grow to the size of its container
                if(!showInActiveSession) {
                    button.layoutParams.width = LayoutParams.MATCH_PARENT
                }
            }

            fun bind(category: Category) {

                // set up short and long click handler for selecting categories
                button.setOnClickListener { shortClickHandler(category, it) }
                button.setOnLongClickListener {
                    // tell the event handler we consumed the event
                    return@setOnLongClickListener longClickHandler(category.id, it)
                }


                // store the id of the category on the button
                button.tag = category.id

                // archived categories should not be displayed
                if (category.archived) {
                    button.visibility = View.GONE
                }

                // contents of the view with that element
                button.text = category.name

                val categoryColors =  context.resources.getIntArray(R.array.category_colors)
                button.backgroundTintList = ColorStateList.valueOf(
                    categoryColors[category.colorIndex]
                )
            }
        }

        class AddNewCategoryViewHolder(
            view: View,
            private val addCategoryHandler: () -> Unit,
        ) : ViewHolder(view) {

            private val button: ImageButton = view.findViewById(R.id.addNewCategory)

            fun bind() {
                button.setOnClickListener { addCategoryHandler() }
            }
        }
    }
}