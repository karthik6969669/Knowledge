package com.example.myapplicationtt;

import android.widget.Filter;

import com.example.myapplicationtt.adapters.AdapterAd;
import com.example.myapplicationtt.models.ModelAd;

import java.util.ArrayList;

public class FilterAd extends Filter {

    private final AdapterAd adapter;
    private final ArrayList<ModelAd> filterList;

    public FilterAd(AdapterAd adapter, ArrayList<ModelAd> filterList) {
        this.adapter = adapter;
        this.filterList = filterList;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();
        ArrayList<ModelAd> filteredModels = new ArrayList<>();

        if (constraint != null && constraint.length() > 0) {
            String filterPattern = constraint.toString().toLowerCase().trim();
            for (ModelAd model : filterList) {
                if (model.getBookName() != null && model.getBookName().toLowerCase().contains(filterPattern) ||
                        model.getCategory() != null && model.getCategory().toLowerCase().contains(filterPattern) ||
                        model.getTitle() != null && model.getTitle().toLowerCase().contains(filterPattern)) {
                    filteredModels.add(model);
                }
            }
            results.count = filteredModels.size();
            results.values = filteredModels;
        } else {
            results.count = filterList.size();
            results.values = filterList;
        }

        return results;
    }

    @Override
    protected void publishResults(CharSequence charSequence, FilterResults results) {
        adapter.adArrayList.clear();
        if (results.values != null) {
            adapter.adArrayList.addAll((ArrayList<ModelAd>) results.values);
        }
        adapter.notifyDataSetChanged();
    }
}
