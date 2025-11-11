package com.dexter.little_smart_chat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.dexter.little_smart_chat.R
import com.dexter.little_smart_chat.data.Character


class CharacterAdapter(
    private val characters: List<Character>,
    private var selectedId: Int,
    private val onCharacterSelected: (Character) -> Unit
) : RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder>() {

    inner class CharacterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: LinearLayout = view.findViewById(R.id.characterItemContainer)
        val avatar: TextView = view.findViewById(R.id.characterAvatar)
        val name: TextView = view.findViewById(R.id.characterName)
        val description: TextView = view.findViewById(R.id.characterDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_character, parent, false)
        return CharacterViewHolder(view)
    }

    override fun onBindViewHolder(holder: CharacterViewHolder, position: Int) {
        val character = characters[position]

        holder.avatar.text = character.avatar
        holder.name.text = character.name
        holder.description.text = character.description

        // Update selection state
        val isSelected = character.id == selectedId
        holder.container.setBackgroundResource(
            if (isSelected) R.drawable.character_item_selected
            else R.drawable.character_item_default
        )

        holder.itemView.setOnClickListener {
            selectedId = character.id
            notifyDataSetChanged()
            onCharacterSelected(character)
        }
    }

    override fun getItemCount() = characters.size
}