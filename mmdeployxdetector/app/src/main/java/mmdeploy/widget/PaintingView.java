/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package mmdeploy.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import java.util.LinkedList;
import java.util.List;


public class PaintingView extends View {

  private final List<DrawListener> listeners = new LinkedList<>();

  public PaintingView(final Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void setListeners(DrawListener callback) {
    listeners.add(callback);
  }

  @Override
  public synchronized void draw(Canvas canvas) {
    super.draw(canvas);
    for (DrawListener listener : listeners) {
      listener.drawListener(canvas);
    }
  }

}
