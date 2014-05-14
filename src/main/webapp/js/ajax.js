var count = 0;
$(document).ready(function () {   
    var url = 'chat';
    $('#login-name').focus();

    // Add this request into the client queue on the server. You should not request the url in AJAX way.
    // If you do that, XMLHttpRequest will be added into the client queue. Of course, you can operate
    // XMLHttpRequest, but it is a little more complex.
    // TODO: Why [0] ?
    $('#comet-frame')[0].src = url;
   
    $("#login-button").click(function() {
        var name = $('#login-name').val();
        var room = $('#login-room').val();
        if(! name.length > 0) {
            $('#system-message').css("color","red");
            $('#login-name').focus();
            return;
        }
        $('#system-message').css("color","#2d2b3d") ;
        $('#system-message').html(name + ':');

        $('#login-button').disabled = true;
        $('#login-form').css("display","none");
        $('#message-form').css("display","");

        var query = 'action=login' + '&name=' + encodeURI($('#login-name').val())+ '&room=' + encodeURI(room);
        var roomname = $('#login-room').val();
        $('#roomname').val(roomname);
        $.ajax({type:"post", url:url, data:query,
            success:function (data, status) {
                $('#message').focus();
            },
            error:function () {
                alert("error occured!!!");
            }
        });
    });    

    $("#post-button").click(function () {
        var message = $('#message').val();
        if(!message > 0) {
            return;
        }
        $('#message').disabled = true;
        $('#post-button').disabled = true;
        var roomname = $('#roomname').val();
        var query =
        'action=post' +
        '&name=' + encodeURI($('#login-name').val()) +
        '&room=' + encodeURI(roomname) +
        '&message=' + encodeURI(message);
        $.ajax({
            type:"post",
            url:url,
            data:query,
            success:function (data, status) {
                $('#message').disabled = false;
                $('#post-button').disabled = false;
                $('#message').focus();
                $('#message').val("");
            },
            error:function () {
                alert("error occured!!!");
            }
        });
    });
});

function update(data) {
    var p = document.createElement('p');
    p.innerHTML = data.name + ':<br/>' + data.message;

    $('#display')[0].appendChild(p);
}