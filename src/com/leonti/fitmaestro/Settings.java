package com.leonti.fitmaestro;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;


public class Settings extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setPreferenceScreen(createPreferenceHierarchy());
    }
    
    private PreferenceScreen createPreferenceHierarchy() {
        // Root
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        
        // Dialog based preferences
        PreferenceCategory dialogBasedPrefCat = new PreferenceCategory(this);
        dialogBasedPrefCat.setTitle(R.string.units_category);
        root.addPreference(dialogBasedPrefCat);
        
        // List preference
        ListPreference listPref = new ListPreference(this);
        listPref.setEntries(R.array.entries_units);
        listPref.setEntryValues(R.array.entries_values_units);
        listPref.setDialogTitle(R.string.weight_units);
        listPref.setKey("units");
        listPref.setTitle(R.string.weight_units_title);
        listPref.setSummary(R.string.weight_units_desc);
        dialogBasedPrefCat.addPreference(listPref);       
        
        return root;
    }
}
