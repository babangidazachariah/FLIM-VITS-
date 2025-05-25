% Given data (assuming you've already extracted the vectors)
veh_per_hr = [100; 200; 400; 200; 400; 600; 1000; 1200; 1600; 1800];
val_25s = [172; 336; 666; 656; 1310; 1955; 1701; 1881; 1897; 1795];
val_30s = [172; 336; 666; 654; 1304; 1949; 1703; 1886; 1870; 1772];
zouFIS = [132; 264; 496; 650; 1292; 1932; 1995; 2158; 2178; 2063];
qwsdFIS = [172; 336; 664; 657; 1309; 1941; 2529; 2439; 2469; 2466];

% Create a line graph
figure;
plot(veh_per_hr, val_25s, 'b-o', 'DisplayName', '25s');
hold on;
plot(veh_per_hr, val_30s, 'r-s', 'DisplayName', '30s');
plot(veh_per_hr, zouFIS, 'g-d', 'DisplayName', 'ZouFIS');
plot(veh_per_hr, qwsdFIS, 'm-^', 'DisplayName', 'qwsdFIS');
xlabel('Vehicles per hour (veh/hr)');
ylabel('Values');
title('Comparison of Different Variables');
legend('Location', 'best');

% Customize the plot (optional)
grid on;
box on;

% Save the plot (optional)
% saveas(gcf, 'line_graph.png'); % Uncomment this line to save the plot as an image

% Display the plot
hold off;
