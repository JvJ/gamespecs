
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
	Object getInitialState(){
		return null;
	}

	Object getCurrentState(){
		return null;
	}

/*
Reset the application to its initial state.
*/
	void reset(){
	}
}

