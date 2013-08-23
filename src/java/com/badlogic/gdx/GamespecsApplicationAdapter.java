
package com.badlogic.gdx;

import com.badlogic.gdx.ApplicationAdapter;

public abstract class GamespecsApplicationAdapter extends ApplicationAdapter
{

@Override
	public void create () {
	}

	@Override
	public void resize (int width, int height) {
	}

	@Override
	public void render () {
	}

	@Override
	public void pause () {
	}

	@Override
	public void resume () {
	}

	@Override
	public void dispose () {
	}
/*
 Return the initial state of the application.
 */
	public Object getInitialState(){
		return null;
	}

	public Object getCurrentState(){
		return null;
	}

/*
Reset the application to its initial state.
*/
	public void reset(){
	}

    /* This version of reset allows you to
       pass a new application state, which replaces
       the "initial state".
     */
        public void reset(Object o){
        }
}

