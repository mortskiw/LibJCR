/******************************************
Livecoding Scenography

(C) 2018 Jonathan Reus
GPL

*******************************************/

/*--------------------------------------
Todo:
>>> A hierarchical scene system for livecoding
>> different layers, a "meta scene" that encompasses all scenes.. things like server booting and that kind of thing, or recordings... and sub-scenes for livecoding or interface designs
>> be able to manage "template" files that are copied as instances
>> use a macro or use a UI for navigation

>^>^> what about file-based macros, to accompany XML-based macros?
>>>> this would make transforming a quick piece of code into a macro much easier!
>>> I could also consider using the Scene editor as an interface for editing & using macros..

>> hmm, the question is for what purpose / why would I want heirarchies of scenes?
--------------------------------------*/


/*_____________________________________________________________
@class
Scenes
A scene system for livecoding. Template files are stored in a
"scenes" directory, and instances are created from those templates
that can be performed and modified at will without transforming the
original.

Works a bit like Macros but on the scale of files. Keeps track of all your
instances within a performance concept.


@usage

f = "".resolveRelative +/+ "scenes";
z = Scenes(f).makeGui;
________________________________________________________________*/

Scenes {
	var scenePath, instancePath, sceneNames;
	var win;
	*new {|scenedir|
		^super.new.init(scenedir);
	}

	init {|scenedir|
		if(scenedir.isNil) { scenedir = Document.current.path.dirname +/+ "_scenes/" };
		scenedir.postln;
		scenePath = scenedir;
		if(File.exists(scenePath).not) { File.mkdir(scenePath) };
		sceneNames = (scenePath +/+ "*.scd").pathMatch.collect {|it|
			PathName(it).fileNameWithoutExtension
		};
		instancePath = scenePath +/+ "instances/";
		if(File.exists(instancePath).not) { File.mkdir(instancePath) };
	}

	makeGui {|position|
		var width = 200, height = 600, lineheight=20, top=0, left=0;
		var styler, decorator, childView;
		var sceneList, sceneName, addBtn, deleteBtn;
		var subView, subStyler;
		if(win.notNil) { win.close };
    if(position.notNil) {
      top = position.y; left = position.x;
    };
    win = Window("Scene Navigator", Rect(left, top, (width+10), 400));
		styler = GUIStyler(win);

		// child view inside window, this is where all the gui views sit
		childView = styler.getWindow("Scenes", win.view.bounds);
		childView.decorator = FlowLayout(childView.bounds, 5@5);

		sceneName = TextField.new(childView, width@lineheight);
		sceneName.action = {|field| addBtn.doAction };

		addBtn = styler.getSizableButton(childView, "+", "+", lineheight@lineheight);
		deleteBtn = styler.getSizableButton(childView, "-", "-", lineheight@lineheight);

		sceneList = ListView(childView, width@200)
		.items_(sceneNames.asArray).value_(nil)
		.stringColor_(Color.white).background_(Color.clear)
		.hiliteColor_(Color.new(0.3765, 0.5922, 1.0000, 0.5));

		addBtn.action = {|btn|
			var idx, newscene = sceneName.value;
			idx = sceneList.items.indexOfEqual(newscene);
			if(idx.notNil) { // a scene by that name already exists
				"A scene with the name % already exists".format(newscene).warn;
			} { // create a new scene
				var scenepath = scenePath +/+ newscene ++ ".scd";
        "new file at % % %".format(scenepath, newscene, sceneName.value).postln;
				File.use(scenepath, "w", {|fp| fp.write("/* New Scene */") });
				sceneList.items = sceneList.items.add(newscene);
			};
		};

		deleteBtn.action = {|btn|
			var dialog,tmp,bounds,warning,scene,scenepath;
			scene = sceneList.items[sceneList.value];
			scenepath = scenePath +/+ scene ++ ".scd";
			warning = "Delete %\nAre you sure?".format(scene);
			bounds = Rect(win.bounds.left, win.bounds.top + 25, win.bounds.width, win.bounds.height);
			dialog = Window.new("Confirm", bounds, false, false);
			dialog.alwaysOnTop_(true);
			dialog.view.decorator = FlowLayout.new(dialog.view.bounds);
			StaticText.new(dialog, 200@40).string_(warning);
			Button.new(dialog, 60@30).string_("Yes").action_({|btn|
				var newitems;
				newitems = sceneList.items.copy; newitems.removeAt(sceneList.value);
				sceneList.items = newitems;
				File.delete(scenepath);
				"Delete %".format(sceneList.items[sceneList.value]).postln;
				dialog.close;
			});
			Button.new(dialog, 60@30).string_("Cancel").action_({|btn|
				"Abort".postln;
				dialog.close;
			});
      dialog.front;
		};


		subStyler = GUIStyler(childView); // styler for subwindow
		subView = subStyler.getWindow("Subwindow", width@600); // subwindow

		sceneList.action_({ |lv| // action when selecting items in the scene list -> build the scene info view
			var btn, radio, createNewInstanceFunc;
			var matching, scene, templatepath;
			scene = lv.items[lv.value];
			templatepath = scenePath +/+ scene ++ ".scd";
			scene.postln;

      // *** BUILD SCENE INFO WINDOW ***
      subView.removeAll; // remove views & layout for previous scene info window
			subView.decorator = FlowLayout(subView.bounds);

      createNewInstanceFunc = {
        // Does an instance of the scene already exist?
        matching = Document.openDocuments.select {|doc| doc.title.contains(scene) };
        matching.postln;
        matching.size.switch(
          0, { // if no, create a new file/instance & open it
            var original, instance;
            instance = instancePath +/+ Date.getDate.stamp ++ "_" ++ scene ++ ".scd";
            File.use(instance, "w", {|fp| fp.write(File.readAllString(templatepath)) });
            Document.open(instance);
          },
          1, { // if yes, open that instance
            Document.open(matching[0].path);
          },
          { // otherwise you have multiple open instances
            var im = matching.select {|doc| doc.title != (scene++".scd") };
            Document.open(im[0].path);
            "Multiple Matching Instances... ".postln;
          }
        );
      };

			btn = styler.getSizableButton(subView, "open template", size: 80@lineheight);
			btn.action = {|btn| Document.open(templatepath) };
      btn = styler.getSizableButton(subView, "new instance", size: 80@lineheight);
      btn.action = createNewInstanceFunc;

      // Radiobuttons
      // No Auto Loading
      // Auto Load Latest Instance
      // Auto Load New Instance (if latest is older than x days)
      radio = RadioButton(subView);


      // Instances List


      // *** END SCENE INFO WINDOW ***



		}); // END SCENELIST ACTION

    ^win.alwaysOnTop_(true).front;
	}
}


