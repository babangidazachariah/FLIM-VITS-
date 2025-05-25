% Load your fuzzy inference system (replace 'your_fis_file.fis' with your actual filename)
fis = readfis('queueLengthWaitingTimeDistanceSpeedController.fis');
fuzzy
% Create an option set
opt = gensurfOptions;

% Specify the output index (assuming "phasDuration" is the second output)
opt.OutputIndex = 1;

% Specify the input indices for "queueLength" and "speed"
% queue = 1 distance = 2 speed = 3
opt.InputIndex = [1 3];  % Use the first and third inputs

% Generate the output surface
gensurf(fis, opt);

