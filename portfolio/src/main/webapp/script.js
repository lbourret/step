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
function addRandomGreeting() {
  const facts =
      ['My favorite color is purple',  'I love french fries', 'I play soccer', 'My favorite ice cream flavor is coffee',
      'Connecticut born and raised', 'I have never watched Game of Thrones'];

  // Pick a random greeting.
  const fact = facts[Math.floor(Math.random() * facts.length)];

  // Add it to the page.
  const factContainer = document.getElementById('fact-container');
  factContainer.innerText = fact;
}

/**
 * Toggles visibility class
 */
function toggleText(divID) {
    var content = document.getElementById(divID);
    content.classList.toggle('visible');
}

/**
 * Retrieves comments from server
 */
function getComment() {
  const limit = document.getElementById('limit').value;
  const sort = document.getElementById('sort').value;
  const searchParam = document.getElementById('searchName').value;

  fetch('/list-comments?limit=' + limit + '&sort=' + sort + '&searchName=' + searchParam).then(response => response.json()).then((comments) => {

    const commentListElement = document.getElementById('comment-container');
    commentListElement.innerHTML = 'COMMENTS: ';
    comments.forEach((comment) => {
        commentListElement.appendChild(createCommentElement(comment));
    })    
  });
}

/** Creates a comment element. */
function createCommentElement(comment) {
  const commentElement = document.createElement('li');
  commentElement.className = 'comment';
  const commentDetails = document.createElement('span');
  commentDetails.className = 'comment';
  const commentBody = document.createElement('span');
  commentBody.className = 'comment';

  // Time
  var date = new Date(comment.timestamp);
  const dateElement = document.createElement('p');
  dateElement.innerText = date.toDateString();

  // Name 
  const nameElement = document.createElement('p');
  nameElement.innerText = comment.name;

  // Text 
  const textElement = document.createElement('p');
  textElement.innerText = comment.text;

  // Delete Button
  const deleteButtonElement = document.createElement('button');
  deleteButtonElement.className = 'smallDefaultButton';
  deleteButtonElement.innerText = 'Delete';
  deleteButtonElement.addEventListener('click', () => {
    deleteComment(comment);

  // Remove the comment from the DOM.
    commentElement.remove();
  });

  commentDetails.appendChild(dateElement);
  commentDetails.appendChild(nameElement);
  commentBody.appendChild(textElement);
  commentElement.appendChild(commentDetails);
  commentElement.appendChild(commentBody);
  commentElement.appendChild(deleteButtonElement);

  return commentElement;
}

/** Set limit's value to limit from URL. */
function initParam(paramName){
  const limit = getURLParam(paramName);

  // Only take URL param if not null
  if (limit){
      document.getElementById(paramName).value = limit;
  }
}

/** Get parameters from URL */
function getURLParam(paramName){
  const params = new URLSearchParams(window.location.search);
  return params.get(paramName);
}

/** 
    Tells the server to delete the comment.
    @param comment specificied comment to delete
*/
aysnc function deleteComment(comment) {
  const params = new URLSearchParams();
  params.append('id', comment.id);
  await fetch('/delete-comment', {method: 'POST', body: params});
  getComment();
}

/** Tells the server to delete all comments. */
async function deleteAllComments() {
  const response = await fetch('/delete-all-comments', {method: 'POST'});
  const comments = await response.text();
  console.log("# Comments Deleted: " + comments);
  getComment();
}
