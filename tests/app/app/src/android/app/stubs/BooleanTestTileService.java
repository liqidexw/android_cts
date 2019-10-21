/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.app.stubs;

import android.content.ComponentName;
import android.service.quicksettings.Tile;

public class BooleanTestTileService extends TestTileService {
    public static final String TAG = "BooleanTestTileService";
    public static final String PKG = "android.app.stubs";
    public static final int ICON_ID = R.drawable.robot;

    public static String getId() {
        return String.format("%s/%s", BooleanTestTileService.class.getPackage().getName(),
                BooleanTestTileService.class.getName());
    }

    public static ComponentName getComponentName() {
        return new ComponentName(BooleanTestTileService.class.getPackage().getName(),
                BooleanTestTileService.class.getName());
    }

    public void toggleState() {
        if (isListening()) {
            Tile tile = sTestTileService.getQsTile();
            switch(tile.getState()) {
                case Tile.STATE_ACTIVE:
                    tile.setState(Tile.STATE_INACTIVE);
                    break;
                case Tile.STATE_INACTIVE:
                    tile.setState(Tile.STATE_ACTIVE);
                    break;
                default:
                    break;
            }
            tile.updateTile();
        }
    }
}