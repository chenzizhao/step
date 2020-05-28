// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * Adds a random greeting to the page.
 */
function findZoe() {
  const locationToLink = {
    "43.5˚N, 79.4˚W":
      "https://www.google.com/maps/place/43%C2%B030'00.0%22N+79%C2%B024'00.0%22W/",
    "a very very dark place": "https://ssh.cloud.google.com/cloudshell",
    "github": "https://github.com/chenzizhao",
    "linkedin": "https://www.linkedin.com/in/chenzizhao/",
    "the pit": "https://skulepedia.ca/wiki/Sandford_Fleming_Atrium"
  };

  // Pick a random location.
  const locations = Object.keys(locationToLink);
  const location = locations[Math.floor(Math.random() * locations.length)];

  // Add it to the page.
  const linkContainer = document.getElementById("link");
  linkContainer.innerText = location;
  linkContainer.href = locationToLink[location];
}

function grow() {
  const imageToAlt = {
    "images/basil1.jpg": "Basil Day 0",
    "images/basil2.jpg": "Basil Day 10",
    "images/basil3.jpg": "Basil Day 20",
    "images/basil4.jpg": "Basil Day 30",
    "images/basil5.jpg": "Basil Day 40",
    "images/basil6.jpg": "Basil Day 60"
  };
  const imgContainer = document.getElementById("basil");
  var imgIndex = 1;
  var image = "images/basil" + imgIndex + ".jpg";
  console.log("Let's grow some basil");

  // Recursive
  function someTimeLater() {
    setTimeout(function() {
      console.log("Some time has passed ...");
      // Change the image
      image = "images/basil" + imgIndex + ".jpg";
      imgContainer.src = image;
      imgContainer.alt = imageToAlt[image];
      imgContainer.width = "400";
      imgContainer.height = "400";
      imgIndex++;
      if (imgIndex <= 6) {
        someTimeLater(); // Keep growing
      } else {
        console.log("That's it for now!");
      }
    }, 1000); // Update the image every 1000 ms = 1 sec
  }

  someTimeLater();
}