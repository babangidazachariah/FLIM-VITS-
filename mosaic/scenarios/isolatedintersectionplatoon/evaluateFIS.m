% Load the FIS structure
fis = readfis('ctrlers/queueDependent.fis');

% Define input values
input1 = 20; % Example input value for 'queueLength'
input2 = 50; % Example input value for 'waitingTime'

% Evaluate the FIS
output = evalfis( fis, [input1, input2]);

% Display the output
disp(['Output (phaseDuration) for inputs [queueLength, waitingTime] = [', num2str(input1), ', ', num2str(input2), '] is: ', num2str(output)]);
