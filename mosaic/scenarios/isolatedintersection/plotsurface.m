% Load your fuzzy inference system (replace 'your_fis_file.fis' with your actual filename)
fis = readfis('queueLengthWaitingTimeDistanceSpeedController.fis');

% Create an option set
opt = gensurfOptions;

% Specify the output index (assuming "phasDuration" is the second output)
opt.OutputIndex = 1;

% Specify the input indices for "queueLength" and "speed"
opt.InputIndex = [2 1];  % Use the first and third inputs

% Generate the output surface
gensurf(fis, opt);

