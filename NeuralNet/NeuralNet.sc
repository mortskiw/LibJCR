//SuperCollider is under the GNU GPL; and so is this class.  
//Nick Collins Oct 2007

//fully connected MultilayerPerceptron (one hidden layer) trained through back propagation, online learning (updates after one training example and not after set)
//see Tom Mitchell, Machine Learning, McGraw-hill, 1996, p.98

//could precalculate large table for sigmoid function with interpolation? 

//biases for units can be represented as the 0th term; the input here is always 1, and only the weight is adjusted
//In this implementation, to avoid creating [1]++[input] every time, the biases are just represented as weights; but must still be updated according to backpropagation


NeuralNet {
	var <nin, <nhidden, <nout;  //fixed when network created
	var <>learningrate;
	var <>weightsh, <>weightso, <>biash, <>biaso;  //weights is an array of arrays   
	var <dk, <dh;  //for backpropagation algorithm
	var <input, <hiddenoutput, <output; //for calculation
	var <trainingepoch;
	var <>isTraining;
	
	*new {arg nin, nhidden, nout, learningrate=0.05, initweight=0.05; 
	 	^super.newCopyArgs(nin, nhidden, nout, learningrate).initNeuralNet(initweight);
	 }
	 
	 *newExisting {arg params;
	 	^super.new.setNN(params);
	 } 
	 
	 //initialise weights randomly
	 initNeuralNet {|initweight|
	 	
		 //between -0.05 and 0.05 recommended
		 weightsh = Array.fill(nhidden,{Array.fill(nin,{initweight.rand2})});
		 weightso = Array.fill(nout,{Array.fill(nhidden,{initweight.rand2})});
		 
		 biash=Array.fill(nhidden,{initweight.rand2});
		 biaso=Array.fill(nout,{initweight.rand2});
		 
		 isTraining=false;
		 
	 }
	 
	 //test : (1 + (Array.fill(200,{|i| (i-100)/10}).neg.exp)).reciprocal.plot;
	 sigmoid {|val|
	 
	 ^(1.0 + (val.neg.exp)).reciprocal;
	 }
	 
	 //given an input, calculate the network output, uses sigmoid function for hidden and output layers
	 calculate {|inputdata|
		var unitinput;
		 
		input=inputdata;
		 
		hiddenoutput = Array.fill(nhidden, {|j|
			 
			//unitinput=0.0;
			 
			//[input, weightsh[j], input *weightsh[j], biash[j]].postln;
			 
			unitinput = ((input * (weightsh[j])).sum) + (biash[j]);
			
			//unitinput.postln;
			
			this.sigmoid(unitinput); 
		});
		
		output = Array.fill(nout, {|j|
		 	
			//unitinput=0.0;
			 
			//[hiddenoutput, weightso[j], (hiddenoutput) * (weightso[j]) , biaso[j]].postln; 
			 
			unitinput = (((hiddenoutput) * (weightso[j]) ).sum) + (biaso[j]);
			
			//unitinput.postln;
			
			this.sigmoid(unitinput); 
		});
		
		^output;

	 }
	 
	 train1 {|inputdata, target|
	 	var tmp, tmp2;
	 	
	 	this.calculate(inputdata);
	 
	 	//now back propogation of error from output-target
	 	
	 	//errors
	 	dk= Array.fill(nout,{|k| tmp=output[k]; tmp*(1-tmp)*((target[k])-tmp)});
	 
	 	dh= Array.fill(nhidden,{|h| 
		 	
		 	tmp2=0.0;
		 	nout.do {|k| tmp2= tmp2+ ((weightso[k][h]) * (dk[k])); };
		 	
		 	tmp=hiddenoutput[h]; 
		 	
		 	tmp*(1-tmp)*(tmp2);

	 	});
	 
	 	//updates - can be made more efficient by multiplying arrays? 
	 
	 	weightsh = Array.fill(nhidden,{|h| tmp2=learningrate*(dh[h]);  
	 		
		 	Array.fill(nin,{|i| tmp= weightsh[h][i];
		 	
		 	tmp + (tmp2*(input[i]))  
		 	 
		 	});
		 	 
	 	 });
	 	
	 	weightso = Array.fill(nout,{|k| tmp2=learningrate*(dk[k]);  
	 		
		 	Array.fill(nhidden,{|h|  tmp= weightso[k][h]; 

		 	tmp + (tmp2* (hiddenoutput[h]))  
		 	
		 	});
		 	
	 	});
	  
		 biash=biash + (dh * learningrate); 
		 
		 biaso=biaso + (dk * learningrate);
	 
	 }
	 
	 //could add differentiated training and validation sets later
	 //conditions on stopping with number of epochs or error
	 trainASAP {|trainingset, errortarget=0.05, maxepochs=100, status=true|
	 	var error, errortotal;
	 	
	 	error=2*errortarget;
	 	
	 	isTraining=true;
	 	
	 	if (trainingset.isNil,{"no training set!".postln; ^nil});
	 	
	 	trainingepoch=0;
	 
	 	//could make a routine with wait times to amortise; would be safer, properly interruptable!
	 	while({(trainingepoch<maxepochs) && (error>errortarget) && (isTraining)},{
	 	
	 	errortotal=0.0;
	 	
	 	trainingset.do{|example| 
	 	
	 	//assumes in separable form
	 	this.train1(example[0],example[1]);
	 	
	 	errortotal = errortotal + ((output-(example[1])).squared.sum); 
	 	};
	 	
	 	error=errortotal; //(trainingset.size) would give average error per training example
	 	
	 	if(status,{ [trainingepoch, error].postln;});
	 	
	 	trainingepoch=trainingepoch+1;
	 	});
	 	
	 	isTraining=false;
	 	
	 }
	
	//uses a routine, interruptable, can set slower waittime for amortisation
	 train {|trainingset, errortarget=0.05, maxepochs=100, status=true, waittime=0.01, betweenexamples= 0.001|
	 	var error, errortotal;
	 	
	 	error=2*errortarget;
	 	
	 	isTraining=true;
	 	
	 	if (trainingset.isNil,{"no training set!".postln; ^nil});
	 	
	 	trainingepoch=0;
	 
	 	//could make a routine with wait times to amortise; safer, properly interruptable!
	 	
	 	{
	 	maxepochs.do {
	 	
	 	if ( ((trainingepoch<maxepochs) && (error>errortarget) && (isTraining)),{
	 	
	 	errortotal=0.0;
	 	
	 	trainingset.do{|example| 
	 	
	 	//assumes in separable form
	 	this.train1(example[0],example[1]);
	 	
	 	betweenexamples.wait;
	 	
	 	errortotal = errortotal + ((output-(example[1])).squared.sum); 
	 	};
	 	
	 	error=errortotal; //(trainingset.size) would give average error per training example
	 	
	 	if(status,{ [trainingepoch, error].postln;});
	 	
	 	trainingepoch=trainingepoch+1;
	 	},{
	 	
	 	//"stop!".postln;
	 	
	 	isTraining=false;
	 	
	 	nil.yield;
	 	
	 	});
	 	
	 	waittime.wait;
	 	}
	 	
	 	}.fork;
	 	
	 	
	 }
	 
	 
	 test {|testset|
	 
	 	var errortotal=0.0;
	 	
	 	testset.do{|example| 
	 	
	 	//assumes in separable form
	 	this.calculate(example[0]);
	 	
	 	errortotal = errortotal + ((output-(example[1])).squared.sum); 
	 	};
	 
	 	^errortotal;
	
	 }
	 
	getNN {
		^[nin,nhidden,nout,weightsh, biash, weightso, biaso, learningrate];
	} 
	
	
	//WARNING, overwrites existing parameters
	setNN {|params|
		
		nin= params[0];
		nhidden=params[1];
		nout=params[2];
		weightsh=params[3];
		biash=params[4];
		weightso=params[5];
		biaso=params[6];
		learningrate=params[7];
		
	} 
	
	//efficiency for later?- fix n inputs as last time, only change m
	
}



