package com.game.select;

import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.view.ViewPagerFragment;
import com.android.view.ViewPagerSwipeable;
import com.game.data.Nivel;
import com.project.main.R;

public class LevelSelectionFragment extends ViewPagerFragment implements OnLevelListener
{	
	private LevelSelectionFragmentListener mCallback;
	
	private List<Nivel> listaNiveles;
	private boolean[] estadoNiveles;
	
	/* SECTION Constructora */
	
	public static final LevelSelectionFragment newInstance(List<Nivel> lista, boolean[] estado)
	{
		LevelSelectionFragment fragment = new LevelSelectionFragment();
		fragment.setParameters(lista, estado);
		return fragment;
	}
	
	private void setParameters(List<Nivel> lista, boolean[] estado)
	{
		listaNiveles = lista;
		estadoNiveles = estado;
	}
	
	public interface LevelSelectionFragmentListener
	{
        public void onLevelSelectionSelectClicked(int level);
    }
	
	/* SECTION M�todos Fragment */
	
	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		mCallback = (LevelSelectionFragmentListener) activity;
	}
	
	@Override
	public void onDetach()
	{
		super.onDetach();
		mCallback = null;
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// Seleccionar Layout
		View rootView = inflater.inflate(R.layout.fragment_game_level_selection_layout, container, false);

		viewPager = (ViewPagerSwipeable) rootView.findViewById(R.id.pagerViewLevelSelection1);
		viewPager.setAdapter(this, getActivity().getSupportFragmentManager(), getActivity().getActionBar());
		
		int i = 0;
		Iterator<Nivel> it = listaNiveles.iterator();
		while(it.hasNext())
		{
			Nivel nivel = it.next();
			
			viewPager.addView(LevelSelectFragment.newInstance(this, nivel, estadoNiveles[i]), getString(nivel.getNombreNivel()));
			
			i++;
		}
        return rootView;
    }
	
	@Override
	public void onDestroyView()
	{
		super.onDestroyView();
		
		viewPager = null;
	}
    
    /* SECTION M�todos abstractos de ViewPagerFragment */
    
    @Override
    public void onPageSelected(int page) { }
    
    /* SECTION M�todos de OnLevelListener */
	
    @Override
	public void onLevelSelected(int level)
	{
		mCallback.onLevelSelectionSelectClicked(level);
	}
}
